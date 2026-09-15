/*
   Copyright 2025 Neaera Consulting LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package j2735api;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import j2735ffm.GeneralCodec;
import j2735ffm.Ieee1609Dot2DataCodec;
import j2735ffm.MessageFrameCodec;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@WebMvcTest(ApiController.class)
public class ApiControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean(name = "messageFrameCodec")
  MessageFrameCodec codec;

  @MockitoBean(name = "dot2Codec")
  Ieee1609Dot2DataCodec dot2Codec;

  @MockitoBean(name = "generalCodec")
  GeneralCodec generalCodec;

  private static final HexFormat HEX = HexFormat.of();
  private static final String SAMPLE_XER = "<MessageFrame><messageId>20</messageId></MessageFrame>";
  private static final byte[] SAMPLE_UPER = HEX.parseHex("8740FE");
  private static final byte[] SAMPLE_OER = HEX.parseHex("0003010203");
  private static final String SOME_PDU = "VehicleEventFlags";

  @Test
  public void healthCheck_returnsOkMessage() throws Exception {
    mockMvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo("I am in good health, thanks for checking.")));
  }

  @Test
  public void xerToUper_returnsUperBytes() throws Exception {
    when(codec.xerToUper(SAMPLE_XER)).thenReturn(SAMPLE_UPER);

    mockMvc.perform(post("/xer/uper/bin")
            .contentType(MediaType.APPLICATION_XML)
            .content(SAMPLE_XER))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
        .andExpect(content().bytes(SAMPLE_UPER));

    verify(codec).xerToUper(SAMPLE_XER);
  }

  @Test
  public void xerToUperHex_returnsHexString() throws Exception {
    when(codec.xerToUper(SAMPLE_XER)).thenReturn(SAMPLE_UPER);

    mockMvc.perform(post("/xer/uper/hex")
            .contentType(MediaType.APPLICATION_XML)
            .content(SAMPLE_XER))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(HEX.formatHex(SAMPLE_UPER))));
  }

  @Test
  public void uperToXer_octetStreamBody_returnsXer() throws Exception {
    when(codec.uperToXer(SAMPLE_UPER)).thenReturn(SAMPLE_XER);

    mockMvc.perform(post("/uper/bin/xer")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .content(SAMPLE_UPER))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(SAMPLE_XER)));
  }

  @Test
  public void uperHexToXer_returnsXer() throws Exception {
    when(codec.uperToXer(SAMPLE_UPER)).thenReturn(SAMPLE_XER);

    mockMvc.perform(post("/uper/hex/xer")
            .contentType(MediaType.TEXT_PLAIN)
            .content(HEX.formatHex(SAMPLE_UPER)))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(SAMPLE_XER)));
  }

  @Test
  public void oerHexToXer_returnsXer() throws Exception {
    when(dot2Codec.oerToXer(SAMPLE_OER)).thenReturn(SAMPLE_XER);

    mockMvc.perform(post("/oer/hex/xer")
            .contentType(MediaType.TEXT_PLAIN)
            .content(HEX.formatHex(SAMPLE_OER)))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(SAMPLE_XER)));
  }

  @Test
  public void xerToOerHex_returnsHexString() throws Exception {
    when(dot2Codec.xerToOer(SAMPLE_XER)).thenReturn(SAMPLE_OER);

    mockMvc.perform(post("/xer/oer/hex")
            .contentType(MediaType.APPLICATION_XML)
            .content(SAMPLE_XER))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(HEX.formatHex(SAMPLE_OER))));
  }

  @Test
  public void xerToUperHexAnyPdu_xmlContentType_delegatesToGeneralCodecXerToUper() throws Exception {
    when(generalCodec.xerToUper(SOME_PDU, SAMPLE_XER)).thenReturn(SAMPLE_UPER);

    mockMvc.perform(post("/xer/uper/hex/" + SOME_PDU)
            .contentType(MediaType.APPLICATION_XML)
            .content(SAMPLE_XER))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(HEX.formatHex(SAMPLE_UPER))));

    verify(generalCodec, never()).uperToXer(anyString(), any(byte[].class));
  }

  @Test
  public void uperHexToXerAnyPdu_textPlainContentType_delegatesToGeneralCodecUperToXer() throws Exception {
    when(generalCodec.uperToXer(SOME_PDU, SAMPLE_UPER)).thenReturn(SAMPLE_XER);

    mockMvc.perform(post("/xer/uper/hex/" + SOME_PDU)
            .contentType(MediaType.TEXT_PLAIN)
            .content(HEX.formatHex(SAMPLE_UPER)))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(SAMPLE_XER)));

    verify(generalCodec, never()).xerToUper(anyString(), anyString());
  }

  @Test
  public void xerToUperHex_codecThrows_propagatesError() {
    when(codec.xerToUper(SAMPLE_XER)).thenThrow(new RuntimeException("conversion failed"));

    assertThrows(Exception.class, () ->
        mockMvc.perform(post("/xer/uper/hex")
            .contentType(MediaType.APPLICATION_XML)
            .content(SAMPLE_XER)));
  }

  @Test
  public void uperHexToXer_malformedHex_throwsIllegalArgumentException() {
    assertThrows(Exception.class, () ->
        mockMvc.perform(post("/uper/hex/xer")
            .contentType(MediaType.TEXT_PLAIN)
            .content("not-valid-hex")));
  }
}
