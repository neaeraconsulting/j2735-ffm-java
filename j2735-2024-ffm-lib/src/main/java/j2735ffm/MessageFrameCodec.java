/*
   Copyright 2025 Neaera Consulting LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0
*/
package j2735ffm;

import java.nio.file.Path;

/**
 * MessageFrame compatibility adapter.
 *
 * @deprecated use the generic, thread-safe {@link Asn1Codec} API.
 */
@Deprecated(forRemoval = false, since = "3.0.0")
public class MessageFrameCodec {

  public static final String MESSAGE_FRAME_PDU = "MessageFrame";

  public final long textBufferSize;
  public final long uperBufferSize;
  public final long errorBufferSize;

  private final Asn1Codec delegate;

  @Deprecated
  public MessageFrameCodec(
      long textBufferSize,
      long uperBufferSize,
      @Deprecated long messageFrameAllocateSize,
      @Deprecated long asnCodecCtxMaxStackSize) {
    this(textBufferSize, uperBufferSize, 256L, Asn1Codec.DEFAULT_LIBRARY_PATH);
  }

  public MessageFrameCodec(
      long textBufferSize,
      long uperBufferSize,
      long errorBufferSize,
      Path libraryPath) {
    this.textBufferSize = textBufferSize;
    this.uperBufferSize = uperBufferSize;
    this.errorBufferSize = errorBufferSize;
    this.delegate = new Asn1Codec(
        textBufferSize, uperBufferSize, errorBufferSize, libraryPath);
  }

  public byte[] convertGeneral(
      byte[] inputBytes,
      String pdu,
      AsnEncoding fromEncoding,
      AsnEncoding toEncoding) {
    return delegate.convert(inputBytes, pdu, fromEncoding, toEncoding);
  }

  public byte[] xerToUper(String xer) {
    return delegate.encodeFromXer(xer, MESSAGE_FRAME_PDU, AsnEncoding.UPER);
  }

  public String uperToXer(byte[] uper) {
    return delegate.decodeToXer(uper, MESSAGE_FRAME_PDU, AsnEncoding.UPER);
  }
}
