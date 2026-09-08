/*
   Copyright 2026 Neaera Consulting LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0
*/
package j2735ffm;

import static generated.convert_h.convert_bytes;

import generated.convert_h;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe converter for any PDU exported by the bundled 2024 ASN.1 module.
 *
 * <p>The native library is loaded once per class loader. Every conversion owns a confined FFM
 * arena, so one codec instance can be shared by concurrent ODE listener threads. Text and binary
 * input/output limits are configurable and are checked before native calls.
 */
public class Asn1Codec {

  public static final Path DEFAULT_LIBRARY_PATH = Paths.get("/usr/lib/libasnapplication.so");
  private static final Logger log = LoggerFactory.getLogger(Asn1Codec.class);

  private final long textBufferSize;
  private final long binaryBufferSize;
  private final long errorBufferSize;
  private final Path libraryPath;

  public Asn1Codec(long textBufferSize, long binaryBufferSize, long errorBufferSize) {
    this(textBufferSize, binaryBufferSize, errorBufferSize, DEFAULT_LIBRARY_PATH);
  }

  /**
   * Creates a generic codec backed by the given native library.
   *
   * @param textBufferSize maximum UTF-8 input/output bytes for text encodings
   * @param binaryBufferSize maximum input/output bytes for UPER, OER, and COER
   * @param errorBufferSize native error buffer bytes
   * @param libraryPath path to {@code libasnapplication.so} or {@code asnapplication.dll}
   */
  public Asn1Codec(
      long textBufferSize,
      long binaryBufferSize,
      long errorBufferSize,
      Path libraryPath) {
    this.textBufferSize = validateBufferSize("textBufferSize", textBufferSize);
    this.binaryBufferSize = validateBufferSize("binaryBufferSize", binaryBufferSize);
    this.errorBufferSize = validateBufferSize("errorBufferSize", errorBufferSize);
    if (libraryPath == null) {
      throw libraryException("NATIVE_LIBRARY_MISSING: library path is null", null);
    }
    this.libraryPath = libraryPath.toAbsolutePath().normalize();
    NativeLibrary.initialize(this.libraryPath);
    log.info(
        "Asn1Codec initialized with textBufferSize={}, binaryBufferSize={}, libraryPath={}",
        textBufferSize,
        binaryBufferSize,
        this.libraryPath);
  }

  /** Converts one ASN.1 PDU between supported encodings. */
  public byte[] convert(byte[] input, String pdu, AsnEncoding from, AsnEncoding to) {
    validateOperation(input, pdu, from, to);

    long inputLimit = from.isBinary() ? binaryBufferSize : textBufferSize;
    if (input.length > inputLimit) {
      throw conversionException(
          pdu,
          from,
          to,
          "INPUT_LIMIT: input message too large; requires %d bytes but limit is %d"
              .formatted(input.length, inputLimit));
    }

    long outputLimit = to.isBinary() ? binaryBufferSize : textBufferSize;
    try (Arena arena = Arena.ofConfined()) {
      // Allocate only the bytes passed to native code, not the configured input limit.
      MemorySegment inputBuffer = arena.allocate(input.length);
      inputBuffer.copyFrom(MemorySegment.ofArray(input));
      MemorySegment outputBuffer = arena.allocate(outputLimit);
      MemorySegment errorBuffer = arena.allocate(errorBufferSize);
      MemorySegment pduName = arena.allocateFrom(pdu, StandardCharsets.UTF_8);
      MemorySegment fromName = arena.allocateFrom(from.getName(), StandardCharsets.UTF_8);
      MemorySegment toName = arena.allocateFrom(to.getName(), StandardCharsets.UTF_8);

      final int outputLength;
      try {
        outputLength = convert_bytes(
            pduName,
            fromName,
            toName,
            inputBuffer,
            input.length,
            outputBuffer,
            outputLimit,
            errorBuffer,
            errorBufferSize);
      } catch (Throwable error) {
        throw conversionException(
            pdu,
            from,
            to,
            "NATIVE_FAILURE: " + error.getMessage(),
            error);
      }

      if (outputLength < 0) {
        throw conversionException(pdu, from, to, errorBuffer.getString(0));
      }
      if (outputLength > outputLimit) {
        throw conversionException(
            pdu,
            from,
            to,
            "OUTPUT_LIMIT: native result %d exceeds configured limit %d"
                .formatted(outputLength, outputLimit));
      }

      byte[] output = new byte[outputLength];
      MemorySegment.copy(outputBuffer, 0, MemorySegment.ofArray(output), 0, outputLength);
      return output;
    }
  }

  /** Decodes one binary or text PDU to canonical XER. */
  public String decodeToXer(byte[] input, String pdu, AsnEncoding encoding) {
    return new String(convert(input, pdu, encoding, AsnEncoding.XER), StandardCharsets.UTF_8);
  }

  /** Encodes XER for one PDU to the requested encoding. */
  public byte[] encodeFromXer(String xer, String pdu, AsnEncoding encoding) {
    if (xer == null) {
      throw conversionException(
          pdu, AsnEncoding.XER, encoding, "INVALID_ARGUMENT: XER input is null");
    }
    return convert(xer.getBytes(StandardCharsets.UTF_8), pdu, AsnEncoding.XER, encoding);
  }

  private void validateOperation(
      byte[] input,
      String pdu,
      AsnEncoding from,
      AsnEncoding to) {
    if (input == null) {
      throw conversionException(pdu, from, to, "INVALID_ARGUMENT: input is null");
    }
    if (input.length == 0) {
      throw conversionException(pdu, from, to, "TRUNCATED_INPUT: input is empty");
    }
    if (pdu == null || pdu.isBlank()) {
      throw conversionException(pdu, from, to, "INVALID_ARGUMENT: PDU is blank");
    }
    if (from == null || !from.isSupported()) {
      throw conversionException(
          pdu, from, to, "INVALID_ENCODING: unsupported source encoding " + from);
    }
    if (to == null || !to.isSupported()) {
      throw conversionException(
          pdu, from, to, "INVALID_ENCODING: unsupported target encoding " + to);
    }
  }

  private static long validateBufferSize(String name, long size) {
    if (size <= 0 || size > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "%s must be between 1 and %d bytes".formatted(name, Integer.MAX_VALUE));
    }
    return size;
  }

  public long getTextBufferSize() {
    return textBufferSize;
  }

  public long getBinaryBufferSize() {
    return binaryBufferSize;
  }

  public long getErrorBufferSize() {
    return errorBufferSize;
  }

  public Path getLibraryPath() {
    return libraryPath;
  }

  private static Asn1CodecException conversionException(
      String pdu,
      AsnEncoding from,
      AsnEncoding to,
      String nativeError) {
    return new Asn1CodecException(pdu, from, to, nativeError);
  }

  private static Asn1CodecException conversionException(
      String pdu,
      AsnEncoding from,
      AsnEncoding to,
      String nativeError,
      Throwable cause) {
    return new Asn1CodecException(pdu, from, to, nativeError, cause);
  }

  private static Asn1CodecException libraryException(String message, Throwable cause) {
    return new Asn1CodecException("<native-library>", null, null, message, cause);
  }

  private static final class NativeLibrary {
    private static Path loadedPath;
    private static Arena libraryArena;

    private NativeLibrary() {
    }

    private static synchronized void initialize(Path requestedPath) {
      if (!Files.isRegularFile(requestedPath)) {
        throw libraryException("NATIVE_LIBRARY_MISSING: " + requestedPath, null);
      }

      final Path realPath;
      try {
        realPath = requestedPath.toRealPath();
      } catch (Exception error) {
        throw libraryException(
            "NATIVE_LIBRARY_LOAD_FAILED: cannot resolve " + requestedPath, error);
      }

      if (loadedPath != null) {
        if (!loadedPath.equals(realPath)) {
          throw libraryException(
              "NATIVE_LIBRARY_ALREADY_LOADED: %s (requested %s)"
                  .formatted(loadedPath, realPath),
              null);
        }
        return;
      }

      try {
        libraryArena = Arena.ofShared();
        SymbolLookup lookup = SymbolLookup.libraryLookup(realPath, libraryArena);
        if (lookup.find("convert_bytes").isEmpty()) {
          throw new UnsatisfiedLinkError("symbol 'convert_bytes' not found");
        }
        convert_h.SYMBOL_LOOKUP = lookup;
        loadedPath = realPath;
        log.info("Loaded ASN.1 native library: {}", realPath);
      } catch (Throwable error) {
        if (libraryArena != null) {
          libraryArena.close();
          libraryArena = null;
        }
        throw libraryException(
            "NATIVE_LIBRARY_LOAD_FAILED: %s: %s".formatted(realPath, error.getMessage()),
            error);
      }
    }
  }
}
