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

import static j2735ffm.AsnEncoding.JER;
import static j2735ffm.AsnEncoding.OER;
import static j2735ffm.AsnEncoding.UPER;
import static j2735ffm.AsnEncoding.XER;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import java.nio.charset.StandardCharsets;
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
class ApiControllerTest {

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
  private static final String SAMPLE_JER = "{\"messageId\":20}";
  private static final byte[] SAMPLE_UPER = HEX.parseHex("8740FE");
  private static final byte[] SAMPLE_OER = HEX.parseHex("0003010203");
  private static final String SOME_PDU = "VehicleEventFlags";

  @Test
  void healthCheck_returnsOkMessage() throws Exception {
    mockMvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo("I am in good health, thanks for checking.")));
  }

  @Test
  void xerToUper_returnsUperBytes() throws Exception {
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
  void xerToUperHex_returnsHexString() throws Exception {
    when(codec.xerToUper(SAMPLE_XER)).thenReturn(SAMPLE_UPER);

    mockMvc.perform(post("/xer/uper/hex")
            .contentType(MediaType.APPLICATION_XML)
            .content(SAMPLE_XER))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(HEX.formatHex(SAMPLE_UPER))));
  }

  @Test
  void uperToXer_octetStreamBody_returnsXer() throws Exception {
    when(codec.uperToXer(SAMPLE_UPER)).thenReturn(SAMPLE_XER);

    mockMvc.perform(post("/uper/bin/xer")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .content(SAMPLE_UPER))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(SAMPLE_XER)));
  }

  @Test
  void uperHexToXer_returnsXer() throws Exception {
    when(codec.uperToXer(SAMPLE_UPER)).thenReturn(SAMPLE_XER);

    mockMvc.perform(post("/uper/hex/xer")
            .contentType(MediaType.TEXT_PLAIN)
            .content(HEX.formatHex(SAMPLE_UPER)))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(SAMPLE_XER)));
  }

  @Test
  void oerHexToXer_returnsXer() throws Exception {
    when(dot2Codec.oerToXer(SAMPLE_OER)).thenReturn(SAMPLE_XER);

    mockMvc.perform(post("/oer/hex/xer")
            .contentType(MediaType.TEXT_PLAIN)
            .content(HEX.formatHex(SAMPLE_OER)))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(SAMPLE_XER)));
  }

  @Test
  void xerToOerHex_returnsHexString() throws Exception {
    when(dot2Codec.xerToOer(SAMPLE_XER)).thenReturn(SAMPLE_OER);

    mockMvc.perform(post("/xer/oer/hex")
            .contentType(MediaType.APPLICATION_XML)
            .content(SAMPLE_XER))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(HEX.formatHex(SAMPLE_OER))));
  }

  @Test
  void xerToUperHexAnyPdu_xmlContentType_delegatesToGeneralCodecXerToUper() throws Exception {
    when(generalCodec.xerToUper(SOME_PDU, SAMPLE_XER)).thenReturn(SAMPLE_UPER);

    mockMvc.perform(post("/xer/uper/hex/" + SOME_PDU)
            .contentType(MediaType.APPLICATION_XML)
            .content(SAMPLE_XER))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(HEX.formatHex(SAMPLE_UPER))));

    verify(generalCodec, never()).uperToXer(anyString(), any(byte[].class));
  }

  @Test
  void uperHexToXerAnyPdu_textPlainContentType_delegatesToGeneralCodecUperToXer() throws Exception {
    when(generalCodec.uperToXer(SOME_PDU, SAMPLE_UPER)).thenReturn(SAMPLE_XER);

    mockMvc.perform(post("/uper/hex/xer/" + SOME_PDU)
            .contentType(MediaType.TEXT_PLAIN)
            .content(HEX.formatHex(SAMPLE_UPER)))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(SAMPLE_XER)));

    verify(generalCodec, never()).xerToUper(anyString(), anyString());
  }

  @Test
  void xerToUperHex_codecThrows_propagatesError() {
    when(codec.xerToUper(SAMPLE_XER)).thenThrow(new RuntimeException("conversion failed"));

    assertThrows(Exception.class, () ->
        mockMvc.perform(post("/xer/uper/hex")
            .contentType(MediaType.APPLICATION_XML)
            .content(SAMPLE_XER)));
  }

  @Test
  void uperHexToXer_malformedHex_throwsIllegalArgumentException() {
    assertThrows(Exception.class, () ->
        mockMvc.perform(post("/uper/hex/xer")
            .contentType(MediaType.TEXT_PLAIN)
            .content("not-valid-hex")));
  }

  @Test
  void jerToUperHex_returnsHexString() throws Exception {
    when(codec.jerToUper(SAMPLE_JER)).thenReturn(SAMPLE_UPER);

    mockMvc.perform(post("/jer/uper/hex")
            .contentType(MediaType.APPLICATION_JSON)
            .content(SAMPLE_JER))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(HEX.formatHex(SAMPLE_UPER))));
  }

  @Test
  void uperHexToJer_returnsJer() throws Exception {
    when(codec.uperToJer(SAMPLE_UPER)).thenReturn(SAMPLE_JER);

    mockMvc.perform(post("/uper/hex/jer")
            .contentType(MediaType.TEXT_PLAIN)
            .content(HEX.formatHex(SAMPLE_UPER)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(equalTo(SAMPLE_JER)));
  }

  @Test
  void jerToOerHex_returnsHexString() throws Exception {
    when(dot2Codec.jerToOer(SAMPLE_JER)).thenReturn(SAMPLE_OER);

    mockMvc.perform(post("/jer/oer/hex")
            .contentType(MediaType.APPLICATION_JSON)
            .content(SAMPLE_JER))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(HEX.formatHex(SAMPLE_OER))));
  }

  @Test
  void oerHexToJer_returnsJer() throws Exception {
    when(dot2Codec.oerToJer(SAMPLE_OER)).thenReturn(SAMPLE_JER);

    mockMvc.perform(post("/oer/hex/jer")
            .contentType(MediaType.TEXT_PLAIN)
            .content(HEX.formatHex(SAMPLE_OER)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(equalTo(SAMPLE_JER)));
  }

  @Test
  void jerToUperHexAnyPdu_delegatesToGeneralCodec() throws Exception {
    when(generalCodec.jerToUper(SOME_PDU, SAMPLE_JER)).thenReturn(SAMPLE_UPER);

    mockMvc.perform(post("/jer/uper/hex/" + SOME_PDU)
            .contentType(MediaType.APPLICATION_JSON)
            .content(SAMPLE_JER))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(HEX.formatHex(SAMPLE_UPER))));
  }

  @Test
  void uperHexToJerAnyPdu_delegatesToGeneralCodec() throws Exception {
    when(generalCodec.uperToJer(SOME_PDU, SAMPLE_UPER)).thenReturn(SAMPLE_JER);

    mockMvc.perform(post("/uper/hex/jer/" + SOME_PDU)
            .contentType(MediaType.TEXT_PLAIN)
            .content(HEX.formatHex(SAMPLE_UPER)))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(SAMPLE_JER)));
  }

  @Test
  void batchConvert_uperHexToJer_returnsOneLinePerMessage() throws Exception {
    byte[] uper2 = HEX.parseHex("0102");
    String jer2 = "{\"messageId\":21}";
    when(generalCodec.convertGeneral(aryEq(SAMPLE_UPER), eq("MessageFrame"), eq(UPER), eq(JER)))
        .thenReturn(SAMPLE_JER.getBytes(StandardCharsets.UTF_8));
    when(generalCodec.convertGeneral(aryEq(uper2), eq("MessageFrame"), eq(UPER), eq(JER)))
        .thenReturn(jer2.getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(post("/batch/uper/jer/MessageFrame")
            .contentType(MediaType.TEXT_PLAIN)
            .content(HEX.formatHex(SAMPLE_UPER) + "\n" + HEX.formatHex(uper2) + "\n"))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(SAMPLE_JER + "\n" + jer2)));
  }

  @Test
  void batchConvert_jerToUperHex_returnsHexLines() throws Exception {
    when(generalCodec.convertGeneral(aryEq(SAMPLE_JER.getBytes(StandardCharsets.UTF_8)),
        eq("MessageFrame"), eq(JER), eq(UPER)))
        .thenReturn(SAMPLE_UPER);

    mockMvc.perform(post("/batch/jer/uper/MessageFrame")
            .contentType(MediaType.TEXT_PLAIN)
            .content(SAMPLE_JER + "\r\n" + SAMPLE_JER))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(
            HEX.formatHex(SAMPLE_UPER) + "\n" + HEX.formatHex(SAMPLE_UPER))));
  }

  @Test
  void batchConvert_failedLine_outputsEmptyLine() throws Exception {
    byte[] badUper = HEX.parseHex("ff");
    when(generalCodec.convertGeneral(aryEq(SAMPLE_UPER), eq("MessageFrame"), eq(UPER), eq(XER)))
        .thenReturn(SAMPLE_XER.getBytes(StandardCharsets.UTF_8));
    when(generalCodec.convertGeneral(aryEq(badUper), eq("MessageFrame"), eq(UPER), eq(XER)))
        .thenThrow(new RuntimeException("conversion failed"));

    String uperHex = HEX.formatHex(SAMPLE_UPER);
    mockMvc.perform(post("/batch/uper/xer/MessageFrame")
            .contentType(MediaType.TEXT_PLAIN)
            .content(uperHex + "\nff\n" + uperHex))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(SAMPLE_XER + "\n\n" + SAMPLE_XER)));
  }

  @Test
  void batchConvert_malformedHexLine_outputsEmptyLine() throws Exception {
    when(generalCodec.convertGeneral(aryEq(SAMPLE_UPER), eq("MessageFrame"), eq(UPER), eq(JER)))
        .thenReturn(SAMPLE_JER.getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(post("/batch/uper/jer/MessageFrame")
            .contentType(MediaType.TEXT_PLAIN)
            .content("not-valid-hex\n" + HEX.formatHex(SAMPLE_UPER)))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo("\n" + SAMPLE_JER)));
  }

  @Test
  void batchConvert_blankLines_areSkipped() throws Exception {
    when(generalCodec.convertGeneral(aryEq(SAMPLE_OER), eq("Ieee1609Dot2Data"), eq(OER), eq(JER)))
        .thenReturn(SAMPLE_JER.getBytes(StandardCharsets.UTF_8));

    String oerHex = HEX.formatHex(SAMPLE_OER);
    mockMvc.perform(post("/batch/oer/jer/Ieee1609Dot2Data")
            .contentType(MediaType.TEXT_PLAIN)
            .content("\n" + oerHex + "\n\n   \n" + oerHex + "\n"))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(SAMPLE_JER + "\n" + SAMPLE_JER)));
  }

  @Test
  void batchConvert_invalidEncoding_returnsBadRequest() throws Exception {
    mockMvc.perform(post("/batch/uper/json/MessageFrame")
            .contentType(MediaType.TEXT_PLAIN)
            .content(HEX.formatHex(SAMPLE_UPER)))
        .andExpect(status().isBadRequest());

    verify(generalCodec, never()).convertGeneral(any(byte[].class), anyString(), any(), any());
  }
}
