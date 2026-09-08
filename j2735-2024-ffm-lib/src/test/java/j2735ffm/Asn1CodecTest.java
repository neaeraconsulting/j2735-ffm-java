/*
   Copyright 2026 Neaera Consulting LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0
*/
package j2735ffm;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class Asn1CodecTest {

  private static final String IEEE_PDU = "Ieee1609Dot2Data";
  private static final String MESSAGE_FRAME_PDU = "MessageFrame";
  private static final String ASD_PDU = "AdvisorySituationData";
  private static final Pattern BYTES = Pattern.compile("<bytes>([0-9A-Fa-f]+)</bytes>");
  private static final HexFormat HEX = HexFormat.of();

  private static Asn1Codec codec;
  private static Path libraryPath;

  @BeforeAll
  static void setup() throws URISyntaxException {
    String library = isWindows()
        ? "j2735ffm/asnapplication.dll"
        : "j2735ffm/libasnapplication.so";
    var resource = Asn1CodecTest.class.getClassLoader().getResource(library);
    if (resource == null) {
      throw new IllegalStateException("Native test library not found: " + library);
    }
    libraryPath = Paths.get(resource.toURI());
    codec = new Asn1Codec(2_000_000L, 200_000L, 1024L, libraryPath);
  }

  @ParameterizedTest
  @CsvSource({
      "signed-bsm.xml,20,<psid>32</psid>,<generationTime>705263211000000</generationTime>",
      "signed-tim.xml,31,<psid>131</psid>,<duration><hours>169</hours></duration>"
  })
  void signedCoerDecodesAndRoundTrips(
      String resource,
      String messageId,
      String expectedField,
      String secondExpectedField) {
    assumeFalse(isWindows(), "Windows native packaging is development-only in 3.0.0");
    byte[] coer = encodedBytes(resource);

    String ieeeXer = codec.decodeToXer(coer, IEEE_PDU, AsnEncoding.COER);
    assertTrue(ieeeXer.contains(expectedField), ieeeXer);
    assertTrue(ieeeXer.contains(secondExpectedField), ieeeXer);
    assertArrayEquals(coer, codec.encodeFromXer(ieeeXer, IEEE_PDU, AsnEncoding.COER));

    String unsecuredHex = elementText(ieeeXer, "unsecuredData").replaceAll("\\s", "");
    String messageFrameXer = codec.decodeToXer(
        HEX.parseHex(unsecuredHex), MESSAGE_FRAME_PDU, AsnEncoding.UPER);
    assertTrue(messageFrameXer.contains("<messageId>" + messageId + "</messageId>"));
  }

  @Test
  void advisorySituationDataUnsignedAndSignedRoundTrips() {
    assumeFalse(isWindows(), "Windows native packaging is development-only in 3.0.0");
    String unsignedMessageFrameHex = resource("TIM_MF.hex");
    String unsignedAsd = advisoryWithBytes(
        element(resource("asd-unsigned.xml"), ASD_PDU), unsignedMessageFrameHex);
    byte[] unsignedUper = codec.encodeFromXer(unsignedAsd, ASD_PDU, AsnEncoding.UPER);
    String unsignedRoundTrip = codec.decodeToXer(unsignedUper, ASD_PDU, AsnEncoding.UPER);
    assertArrayEquals(
        unsignedUper,
        codec.encodeFromXer(unsignedRoundTrip, ASD_PDU, AsnEncoding.UPER));

    byte[] signedIeee = encodedBytes("signed-tim.xml");
    String signedAsd = advisoryWithBytes(unsignedAsd, HEX.formatHex(signedIeee));
    byte[] signedUper = codec.encodeFromXer(signedAsd, ASD_PDU, AsnEncoding.UPER);
    String signedRoundTrip = codec.decodeToXer(signedUper, ASD_PDU, AsnEncoding.UPER);
    String signedRoundTripHex = elementText(signedRoundTrip, "advisoryMessage")
        .replaceAll("\\s", "");
    assertArrayEquals(signedIeee, HEX.parseHex(signedRoundTripHex));
    assertTrue(codec.decodeToXer(
        signedIeee, IEEE_PDU, AsnEncoding.COER).contains("<signedData>"));
  }

  @Test
  void advisorySituationDataEnforcesRegionMaximum() {
    assumeFalse(isWindows(), "Windows native packaging is development-only in 3.0.0");
    String validWrapper = resource("asd-16-regions.xml");
    byte[] validMessageFrame = codec.encodeFromXer(
        odePayloadToAsnXer(element(validWrapper, MESSAGE_FRAME_PDU)),
        MESSAGE_FRAME_PDU,
        AsnEncoding.UPER);
    String valid = advisoryWithBytes(
        element(validWrapper, ASD_PDU), HEX.formatHex(validMessageFrame));
    byte[] encoded = codec.encodeFromXer(valid, ASD_PDU, AsnEncoding.UPER);
    assertTrue(encoded.length > 0);

    String invalid = odePayloadToAsnXer(
        element(resource("asd-17-regions-invalid.xml"), MESSAGE_FRAME_PDU));
    Asn1CodecException error = assertThrows(
        Asn1CodecException.class,
        () -> codec.encodeFromXer(invalid, MESSAGE_FRAME_PDU, AsnEncoding.UPER));
    assertTrue(
        error.getNativeError().startsWith("MALFORMED_INPUT")
            || error.getNativeError().startsWith("CONSTRAINT_INVALID"),
        error.getNativeError());
  }

  @Test
  void failuresRetainOperationContextAndStableCategory() {
    byte[] messageFrame = HEX.parseHex(resource("BSM_MF.hex"));
    Asn1CodecException unknownPdu = assertThrows(
        Asn1CodecException.class,
        () -> codec.convert(messageFrame, "NotAPdu", AsnEncoding.UPER, AsnEncoding.XER));
    assertEquals("NotAPdu", unknownPdu.getPdu());
    assertEquals(AsnEncoding.UPER, unknownPdu.getSourceEncoding());
    assertEquals(AsnEncoding.XER, unknownPdu.getTargetEncoding());
    assertTrue(unknownPdu.getNativeError().startsWith("UNKNOWN_PDU"));

    Asn1CodecException empty = assertThrows(
        Asn1CodecException.class,
        () -> codec.decodeToXer(new byte[0], MESSAGE_FRAME_PDU, AsnEncoding.UPER));
    assertTrue(empty.getNativeError().startsWith("TRUNCATED_INPUT"));

    Asn1Codec tinyOutput = new Asn1Codec(32L, 200_000L, 1024L, libraryPath);
    Asn1CodecException outputLimit = assertThrows(
        Asn1CodecException.class,
        () -> tinyOutput.decodeToXer(messageFrame, MESSAGE_FRAME_PDU, AsnEncoding.UPER));
    assertTrue(outputLimit.getNativeError().startsWith("OUTPUT_LIMIT"));
  }

  @Test
  void truncatedCoerFailsDeterministically() {
    assumeFalse(isWindows(), "Windows native packaging is development-only in 3.0.0");
    byte[] full = encodedBytes("signed-bsm.xml");
    byte[] truncated = Arrays.copyOf(full, full.length - 1);
    Asn1CodecException error = assertThrows(
        Asn1CodecException.class,
        () -> codec.decodeToXer(truncated, IEEE_PDU, AsnEncoding.COER));
    assertTrue(
        error.getNativeError().startsWith("TRUNCATED_INPUT")
            || error.getNativeError().startsWith("MALFORMED_INPUT"),
        error.getNativeError());
  }

  @Test
  void missingNativeLibraryFailsBeforeConversion() {
    Asn1CodecException error = assertThrows(
        Asn1CodecException.class,
        () -> new Asn1Codec(1024L, 1024L, 256L, Path.of("does-not-exist.so")));
    assertTrue(error.getNativeError().startsWith("NATIVE_LIBRARY_MISSING"));
  }

  @Test
  void concurrentConversionsUseOneCodecInstance() throws Exception {
    byte[] messageFrame = HEX.parseHex(resource("BSM_MF.hex"));
    try (var executor = Executors.newFixedThreadPool(8)) {
      List<Callable<String>> operations = new ArrayList<>();
      for (int i = 0; i < 64; i++) {
        operations.add(() -> codec.decodeToXer(
            messageFrame, MESSAGE_FRAME_PDU, AsnEncoding.UPER));
      }
      List<Future<String>> results = executor.invokeAll(operations);
      for (Future<String> result : results) {
        assertTrue(result.get().contains("<BasicSafetyMessage>"));
      }
    }
  }

  private static byte[] encodedBytes(String name) {
    Matcher matcher = BYTES.matcher(resource(name));
    if (!matcher.find()) {
      throw new IllegalArgumentException("No encoded bytes in " + name);
    }
    return HEX.parseHex(matcher.group(1));
  }

  private static String element(String xml, String name) {
    String open = "<" + name + ">";
    String close = "</" + name + ">";
    int start = xml.indexOf(open);
    int end = xml.indexOf(close, start);
    if (start < 0 || end < 0) {
      throw new IllegalArgumentException("Missing " + name);
    }
    return xml.substring(start, end + close.length());
  }

  private static String elementText(String xml, String name) {
    String full = element(xml, name);
    return full.substring(full.indexOf('>') + 1, full.lastIndexOf('<'));
  }

  private static String advisoryWithBytes(String asdXer, String encodedBytes) {
    return asdXer.replaceFirst(
        "(?s)<advisoryMessage>.*?</advisoryMessage>",
        Matcher.quoteReplacement(
            "<advisoryMessage>" + encodedBytes + "</advisoryMessage>"));
  }

  private static String odePayloadToAsnXer(String xml) {
    return xml
        .replaceAll("<[^>]+>null</[^>]+>", "")
        .replaceAll("<([A-Za-z][A-Za-z0-9-]*)></\\1>", "<$1/>");
  }

  private static String resource(String name) {
    try {
      return IOUtils.resourceToString("/j2735ffm/" + name, UTF_8);
    } catch (IOException error) {
      throw new IllegalArgumentException("Unable to load " + name, error);
    }
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }
}
