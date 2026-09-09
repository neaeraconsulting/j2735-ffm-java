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
package j2735ffm;

import static j2735ffm.AsnEncoding.OER;
import static j2735ffm.AsnEncoding.UPER;
import static j2735ffm.AsnEncoding.XER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class GeneralCodecTest {

  static final long TEXT_BUFFER_SIZE = 262144L;
  static final long BINARY_BUFFER_SIZE = 8192L;
  static final long ERROR_BUFFER_SIZE = 256L;

  static GeneralCodec codec;
  final static HexFormat hexFormat = HexFormat.of();

  private static final String VEHICLE_EVENT_FLAGS_PDU = "VehicleEventFlags";
  private static final String VEHICLE_EVENT_FLAGS_UPER = "8740FE";
  private static final String VEHICLE_EVENT_FLAGS_XER =
      "<VehicleEventFlags>10000001111111</VehicleEventFlags>";

  private static final String IEEE_1609_PDU = Ieee1609Dot2DataCodec.IEEE1609_DOT2_DATA_PDU;

  private static final String UNSECURED_XER =
      "<Ieee1609Dot2Data><protocolVersion>3</protocolVersion><content>"
          + "<unsecuredData>0102030405</unsecuredData></content></Ieee1609Dot2Data>";

  static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }

  @BeforeAll
  public static void setup() {
    String libResource = isWindows() ? "j2735ffm/asnapplication.dll" : "j2735ffm/libasnapplication.so";
    URL url = GeneralCodecTest.class.getClassLoader().getResource(libResource);
    log.info("Loading library {}", libResource);
    if (url == null) {
      throw new RuntimeException("libasnapplication not found");
    }
    try {
      Path libPath = Paths.get(url.toURI());
      codec = new GeneralCodec(TEXT_BUFFER_SIZE, BINARY_BUFFER_SIZE, ERROR_BUFFER_SIZE, libPath);
      log.info("Created codec");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testLibraryLoaded() {
    assertThat(codec, notNullValue());
    assertThat(codec.textBufferSize, equalTo(TEXT_BUFFER_SIZE));
    assertThat(codec.binaryBufferSize, equalTo(BINARY_BUFFER_SIZE));
    assertThat(codec.errorBufferSize, equalTo(ERROR_BUFFER_SIZE));
  }

  @Test
  public void convertGeneral_uperToXer_messageFrame() {
    byte[] input = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER);
    byte[] result = codec.convertGeneral(input, VEHICLE_EVENT_FLAGS_PDU, UPER, XER);
    assertThat(result, notNullValue());
    String xer = new String(result, StandardCharsets.UTF_8);
    assertThat(xer, equalTo(VEHICLE_EVENT_FLAGS_XER));
  }

  @ParameterizedTest
  @MethodSource("ieee1609OerHex")
  public void convertGeneral_oerToXer_and_back_ieee1609(final String oerHex) {
    byte[] oer = hexNoWs(oerHex);
    byte[] xerBytes = codec.convertGeneral(oer, IEEE_1609_PDU, OER, XER);
    assertThat("xer is null", xerBytes, notNullValue());
    String xer = new String(xerBytes, StandardCharsets.UTF_8);
    log.info("xer: {}", xer);
    assertThat(xer, containsString("<Ieee1609Dot2Data>"));

    byte[] roundTrip = codec.convertGeneral(xerBytes, IEEE_1609_PDU, XER, OER);
    assertThat("round trip oer differs", hexFormat.formatHex(roundTrip),
        equalToIgnoringCase(oerHex.replaceAll("\\s", "")));
  }

  @Test
  public void xerToOer_oerToXer_explicitPdu() {
    byte[] oer = codec.xerToOer(IEEE_1609_PDU, UNSECURED_XER);
    assertThat(oer, notNullValue());
    String xer = codec.oerToXer(IEEE_1609_PDU, oer);
    assertThat(xer, containsString("<unsecuredData>0102030405</unsecuredData>"));
    byte[] roundTrip = codec.xerToOer(IEEE_1609_PDU, xer);
    assertThat(hexFormat.formatHex(roundTrip), equalToIgnoringCase(hexFormat.formatHex(oer)));
  }

  @Test
  public void xerToUper_uperToXer_explicitPdu() {
    String xer = loadResource("SPAT_MF.xml");
    byte[] uper = codec.xerToUper(MessageFrameCodec.MESSAGE_FRAME_PDU, xer);
    assertThat(uper, notNullValue());
    String roundTripXer = codec.uperToXer(MessageFrameCodec.MESSAGE_FRAME_PDU, uper);
    byte[] roundTripUper = codec.xerToUper(MessageFrameCodec.MESSAGE_FRAME_PDU, roundTripXer);
    assertThat("round trip uper differs", hexFormat.formatHex(roundTripUper),
        equalToIgnoringCase(hexFormat.formatHex(uper)));
  }

  @Test
  public void convertGeneral_badPdu_throws() {
    byte[] input = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER);
    assertThrows(
        RuntimeException.class,
        () -> codec.convertGeneral(input, "BadPDU", UPER, XER)
    );
  }

  private static Stream<String> ieee1609OerHex() {
    return Stream.of(
        loadResource("Ieee1609Dot2Data_unsecured_bsm.coer.hex"),
        loadResource("Ieee1609Dot2Data_signed.hex")
    );
  }

  private static byte[] hexNoWs(String hex) {
    return hexFormat.parseHex(hex.replaceAll("\\s", ""));
  }

  protected static String loadResource(String name) {
    try {
      return IOUtils.resourceToString("/j2735ffm/" + name, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
