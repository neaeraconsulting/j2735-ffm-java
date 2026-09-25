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

import static j2735ffm.AsnEncoding.UPER;
import static j2735ffm.AsnEncoding.XER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HexFormat;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
class MessageFrameCodecTest extends BaseCodecTest {

  private static final String SSM_PDU = "SignalStatusMessage";
  private static final String MALFORMED_SSM = "65e539";

  static MessageFrameCodec codec;

  @BeforeAll
  static void setup() {
    codec = new MessageFrameCodec(TEXT_BUFFER_SIZE, BINARY_BUFFER_SIZE, ERROR_BUFFER_SIZE, getLibPath());
  }

  @Test
  void testLibraryLoaded() {
    assertThat(codec, notNullValue());
    log.debug("Library loaded");
  }

  @Test
  void testXerToUper() {
    final String xer = loadResource("SPAT_MF.xml");
    byte[] uper = codec.xerToUper(xer);
    assertThat(uper, notNullValue());
    String hex = hexFormat.formatHex(uper);
    log.debug("hex: {}", hex);
  }

  @Test
  void uperToXerNoConstraintCheck_matchesUperToXer() {
    byte[] uper = hexFormat.parseHex(loadResource("BSM_MF.hex"));
    String xer = codec.uperToXerNoConstraintCheck(uper);
    assertThat(xer, notNullValue());
    assertThat(xer, equalTo(codec.uperToXer(uper)));
  }

  @ParameterizedTest
  @MethodSource("messageFrameHex")
  void uperToXer(final String uper) {
    // Normalize case
    String xer = codec.uperToXer(HexFormat.of().parseHex(uper));
    assertThat("xer is null", xer, notNullValue());
    log.debug("xer: {}", xer);
    byte[] roundTripUper = codec.xerToUper(xer);
    String roundTripUperHex = hexFormat.formatHex(roundTripUper);
    log.debug("round trip uper: {}", roundTripUper);
    assertThat("round trip hex differs", roundTripUperHex, equalToIgnoringCase(uper));
  }

  @ParameterizedTest
  @MethodSource("messageFrameHex")
  void uperToJer(final String uper) {
    log.debug("uper: {}", uper);
    String jer = codec.uperToJer(HexFormat.of().parseHex(uper));
    assertThat("jer is null", jer, notNullValue());
    log.debug("jer: {}", jer);
    byte[] roundTripUper = codec.jerToUper(jer);
    String roundTripUperHex = hexFormat.formatHex(roundTripUper);
    log.debug("round trip uper: {}", roundTripUperHex);
    assertThat("round trip hex differs", roundTripUperHex, equalToIgnoringCase(uper));
  }

  @Test
  void uperToJerNoConstraintCheck_matchesUperToJer() {
    byte[] uper = hexFormat.parseHex(loadResource("BSM_MF.hex"));
    log.debug("uper: {}", hexFormat.formatHex(uper));
    String jer = codec.uperToJerNoConstraintCheck(uper);
    assertThat(jer, notNullValue());
    log.debug("jer: {}", jer);
    assertThat(jer, equalTo(codec.uperToJer(uper)));
  }




  @Test
  void malformedUperError() {
    byte[] inputBytes = hexFormat.parseHex(MALFORMED_SSM);
    assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, SSM_PDU, UPER, XER);
        }
    );
  }

  @Test
  void testConvertGeneral_InputTooBig() {
    byte[] inputBytes = new byte[10000];
    RuntimeException re = assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, SSM_PDU, UPER, XER);
        }
    );
    assertThat(re.getMessage(), containsString("too large"));
  }

  @Test
  void testUperToXer_InputTooBig() {
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
  void testXerToUper_InputTooBig() {
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

}
