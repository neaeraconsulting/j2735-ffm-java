/*
   Copyright 2026 Neaera Consulting LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0
*/
package j2735ffm;

/**
 * Conversion failure with the complete ASN.1 operation context.
 *
 * <p>{@link #getNativeError()} begins with a stable category such as
 * {@code MALFORMED_INPUT}, {@code TRUNCATED_INPUT}, {@code CONSTRAINT_INVALID},
 * {@code UNKNOWN_PDU}, or {@code OUTPUT_LIMIT} when the failure is reported by the native codec.
 */
public class Asn1CodecException extends RuntimeException {

  private final String pdu;
  private final AsnEncoding sourceEncoding;
  private final AsnEncoding targetEncoding;
  private final String nativeError;

  public Asn1CodecException(
      String pdu,
      AsnEncoding sourceEncoding,
      AsnEncoding targetEncoding,
      String nativeError) {
    this(pdu, sourceEncoding, targetEncoding, nativeError, null);
  }

  public Asn1CodecException(
      String pdu,
      AsnEncoding sourceEncoding,
      AsnEncoding targetEncoding,
      String nativeError,
      Throwable cause) {
    super(buildMessage(pdu, sourceEncoding, targetEncoding, nativeError), cause);
    this.pdu = pdu;
    this.sourceEncoding = sourceEncoding;
    this.targetEncoding = targetEncoding;
    this.nativeError = nativeError;
  }

  private static String buildMessage(
      String pdu,
      AsnEncoding sourceEncoding,
      AsnEncoding targetEncoding,
      String nativeError) {
    return "ASN.1 conversion failed for %s (%s -> %s): %s"
        .formatted(pdu, sourceEncoding, targetEncoding, nativeError);
  }

  public String getPdu() {
    return pdu;
  }

  public AsnEncoding getSourceEncoding() {
    return sourceEncoding;
  }

  public AsnEncoding getTargetEncoding() {
    return targetEncoding;
  }

  public String getNativeError() {
    return nativeError;
  }
}
