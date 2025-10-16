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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class MessageFrameCodecTest {

  static MessageFrameCodec codec;
  final static HexFormat hexFormat = HexFormat.of();


  static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }

  @BeforeAll
  public static void setup() {

    String libResource = isWindows() ? "j2735ffm/asnapplication.dll" : "j2735ffm/libasnapplication.so";
    URL url = MessageFrameCodecTest.class.getClassLoader().getResource(libResource);
    log.info("Loading library {}", libResource);

    if (url == null) {
      throw new RuntimeException("libasnapplication not found");
    }
    try {
      Path libPath = Paths.get(url.toURI());
      codec = new MessageFrameCodec(262144L, 8192L, 256L, libPath);
      log.info("Created codec");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

  }

  @Test
  public void testLibraryLoaded() {
    assertThat(codec, notNullValue());
    log.info("Library loaded");
  }

  @Test
  public void testXerToUper() {
    final String xer = loadResource("SPAT_MF.xml");
    byte[] uper = codec.xerToUper(xer);
    assertThat(uper, notNullValue());
    String hex = hexFormat.formatHex(uper);
    log.info("hex: {}", hex);
  }

  @ParameterizedTest
  @MethodSource("messageFrameHex")
  public void uperToXer(final String uper) {
    // Normalize case
    String xer = codec.uperToXer(HexFormat.of().parseHex(uper));
    assertThat("xer is null", xer, notNullValue());
    log.info("xer: {}", xer);
    byte[] roundTripUper = codec.xerToUper(xer);
    String roundTripUperHex = hexFormat.formatHex(roundTripUper);
    log.info("round trip uper: {}", roundTripUper);
    assertThat("round trip hex differs", roundTripUperHex, equalToIgnoringCase(uper));
  }

  @ParameterizedTest
  @MethodSource("convertData")
  public void testConvertGeneral(final String pdu, final String inputHex, final String expectXer) {
    byte[] inputBytes = hexFormat.parseHex(inputHex);
    byte[] result = codec.convertGeneral(inputBytes, pdu, "uper", "xer");
    assertThat("result is null", result, notNullValue());
    if (expectXer != null) {
      String xer = new String(result, StandardCharsets.UTF_8);
      assertThat(xer, equalTo(expectXer));
    }
  }

  @Test
  public void invalidPduError() {
    byte[] inputBytes = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER);
    assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, "BadPDU", "uper", "xer");
        }
    );
  }

  @Test
  public void invalidInputEncodingError() {
    byte[] inputBytes = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER);
    assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, VEHICLE_EVENT_FLAGS_PDU, "badinputencoding", "xer");
        }
    );
  }

  @Test
  public void invalidOutputEncodingError() {
    byte[] inputBytes = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER);
    assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, VEHICLE_EVENT_FLAGS_PDU, "uper", "badoutputencoding");
        }
    );
  }

  @Test
  public void malformedUperError() {
    byte[] inputBytes = hexFormat.parseHex(MALFORMED_SSM);
    assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, SSM_PDU, "uper", "xer");
        }
    );
  }

  @Test
  public void testConvertGeneral_InputTooBig() {
    byte[] inputBytes = new byte[10000];
    RuntimeException re = assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, SSM_PDU, "uper", "xer");
        }
    );
    assertThat(re.getMessage(), containsString("too large"));
  }

  @Test
  public void testUperToXer_InputTooBig() {
    byte[] inputBytes = new byte[10000];
    RuntimeException re = assertThrows(
        RuntimeException.class,
        () -> {
          codec.uperToXer(inputBytes);
        }
    );
    assertThat(re.getMessage(), containsString("too large"));;
  }

  @Test
  public void testXerToUper_InputTooBig() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 262144L + 10; i++) {
      sb.append("A");
    }
    String inputXer = sb.toString();
    RuntimeException re = assertThrows(
        RuntimeException.class,
        () -> {
          codec.xerToUper(inputXer);
        }
    );
    assertThat(re.getMessage(), containsString("too large"));;
  }

  private static Stream<Arguments> convertData() {
    return Stream.of(
        Arguments.of(VEHICLE_EVENT_FLAGS_PDU, VEHICLE_EVENT_FLAGS_UPER, VEHICLE_EVENT_FLAGS_XER),
        Arguments.of(SSM_PDU, loadResource("SSM.hex"), null)
    );
  }

  private static Stream<Arguments> messageFrameHex() {
    return Stream.of(
      Arguments.of(loadResource("BSM_MF.hex")),
        Arguments.of(loadResource("MAP_MF.hex")),
        Arguments.of(loadResource("PSM_MF.hex")),
        Arguments.of(loadResource("RSM_MF.hex")),
        Arguments.of(loadResource("SDSM_MF.hex")),
        Arguments.of(loadResource("SDSM_MF.hex")),
        Arguments.of(loadResource("SPAT1_MF.hex")),
        Arguments.of(loadResource("SPAT2_MF.hex")),
        Arguments.of(loadResource("SRM_MF.hex")),
        Arguments.of(loadResource("SSM_MF.hex")),
        Arguments.of(loadResource("TIM_MF.hex"))
    );
  }

  protected static String loadResource(String name) {
    String str;
    try {
      str = IOUtils.resourceToString("/j2735ffm/" + name, StandardCharsets.UTF_8);
      log.debug("Loaded resource: {}", str);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return str;
  }

  private static final String VEHICLE_EVENT_FLAGS_UPER = "8740FE";
  private static final String VEHICLE_EVENT_FLAGS_XER = "<VehicleEventFlags>10000001111111</VehicleEventFlags>";
  private static final String VEHICLE_EVENT_FLAGS_PDU = "VehicleEventFlags";

  private static final String SSM_PDU = "SignalStatusMessage";
  private static final String MALFORMED_SSM = "65e539";

}
