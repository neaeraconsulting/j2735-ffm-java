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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
class Ieee1609Dot2DataCodecTest extends BaseCodecTest {

  static Ieee1609Dot2DataCodec codec;

  private static final String UNSECURED_XER =
      "<Ieee1609Dot2Data><protocolVersion>3</protocolVersion><content>"
          + "<unsecuredData>0102030405</unsecuredData></content></Ieee1609Dot2Data>";

  @BeforeAll
  static void setup() {
    codec = new Ieee1609Dot2DataCodec(TEXT_BUFFER_SIZE, BINARY_BUFFER_SIZE, ERROR_BUFFER_SIZE, getLibPath());
  }

  @Test
  void testLibraryLoaded() {
    assertThat(codec, notNullValue());
  }

  @Test
  void oerToXer_unsecuredBsm() {
    String xer = codec.oerToXer(hexNoWs(loadResource("Ieee1609Dot2Data_unsecured_bsm.coer.hex")));
    assertThat(xer, notNullValue());
    assertThat(xer, containsString("<Ieee1609Dot2Data>"));
    assertThat(xer, containsString("<unsecuredData>"));
  }

  @Test
  void oerToXer_signed() {
    String xer = codec.oerToXer(hexNoWs(loadResource("Ieee1609Dot2Data_signed.hex")));
    log.info("xer from oer: {}", xer);
    assertThat(xer, notNullValue());
    assertThat(xer, containsString("<signedData>"));
  }

  @Test
  void oerToXerNoConstraintCheck_matchesOerToXer() {
    byte[] oer = hexNoWs(loadResource("Ieee1609Dot2Data_unsecured_bsm.coer.hex"));
    String xer = codec.oerToXerNoConstraintCheck(oer);
    assertThat(xer, notNullValue());
    assertThat(xer, equalTo(codec.oerToXer(oer)));
  }

  @ParameterizedTest
  @MethodSource("oerHexFixtures")
  void oerToXer_xerToOer_roundTrip(final String oerHex) {
    byte[] oer = hexNoWs(oerHex);
    String xer = codec.oerToXer(oer);
    assertThat(xer, notNullValue());
    byte[] roundTrip = codec.xerToOer(xer);
    assertThat("round trip oer differs", hexFormat.formatHex(roundTrip),
        equalToIgnoringCase(oerHex.replaceAll("\\s", "")));
  }

  @ParameterizedTest
  @MethodSource("oerHexFixtures")
  void oerToJer_jerToOer_roundTrip(final String oerHex) {
    byte[] oer = hexNoWs(oerHex);
    log.info("oer: {}", hexFormat.formatHex(oer));
    String jer = codec.oerToJer(oer);
    assertThat(jer, notNullValue());
    log.info("jer: {}", jer);
    byte[] roundTrip = codec.jerToOer(jer);
    assertThat("round trip oer differs", hexFormat.formatHex(roundTrip),
        equalToIgnoringCase(oerHex.replaceAll("\\s", "")));
  }

  @Test
  void oerToJerNoConstraintCheck_matchesOerToJer() {
    byte[] oer = hexNoWs(loadResource("Ieee1609Dot2Data_unsecured_bsm.coer.hex"));
    log.info("oer: {}", hexFormat.formatHex(oer));
    String jer = codec.oerToJerNoConstraintCheck(oer);
    assertThat(jer, notNullValue());
    log.info("jer: {}", jer);
    assertThat(jer, equalTo(codec.oerToJer(oer)));
  }

  @Test
  void xerToOer_fromSyntheticXer() {
    byte[] oer = codec.xerToOer(UNSECURED_XER);
    assertThat(oer, notNullValue());
    String xer = codec.oerToXer(oer);
    assertThat(xer, containsString("<unsecuredData>0102030405</unsecuredData>"));
    byte[] roundTrip = codec.xerToOer(xer);
    assertThat(hexFormat.formatHex(roundTrip), equalToIgnoringCase(hexFormat.formatHex(oer)));
  }

  private static Stream<String> oerHexFixtures() {
    return Stream.of(
        loadResource("Ieee1609Dot2Data_unsecured_bsm.coer.hex"),
        loadResource("Ieee1609Dot2Data_signed.hex")
    );
  }




}
