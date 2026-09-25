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

import static j2735ffm.AsnEncoding.INVALID;
import static j2735ffm.AsnEncoding.JER;
import static j2735ffm.AsnEncoding.OER;
import static j2735ffm.AsnEncoding.UPER;
import static j2735ffm.AsnEncoding.XER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
class GeneralCodecTest extends BaseCodecTest {

  static GeneralCodec codec;

  private static final String VEHICLE_EVENT_FLAGS_PDU = "VehicleEventFlags";
  private static final String VEHICLE_EVENT_FLAGS_UPER_14BITS = "8740FE";
  private static final String VEHICLE_EVENT_FLAGS_XER_14BITS =
      "<VehicleEventFlags>10000001111111</VehicleEventFlags>";
  private static final String VEHICLE_EVENT_FLAGS_JER_14BITS = """
      {"value":"81FC","length":14}""";
  private static final String VEHICLE_EVENT_FLAGS_UPER_13BITS = "4004";
  private static final String VEHICLE_EVENT_FLAGS_JER_13BITS = """
      {"value":"8008","length":13}""";
  private static final String VEHICLE_EVENT_FLAGS_XER_13BITS = "<VehicleEventFlags>1000000000001</VehicleEventFlags>";

  private static final String IEEE_1609_PDU = Ieee1609Dot2DataCodec.IEEE1609_DOT2_DATA_PDU;

  private static final String SSM_PDU = "SignalStatusMessage";
  private static final String MALFORMED_SSM_UPER = "65e539";

  private static final String UNSECURED_XER =
      "<Ieee1609Dot2Data><protocolVersion>3</protocolVersion><content>"
          + "<unsecuredData>0102030405</unsecuredData></content></Ieee1609Dot2Data>";

  @BeforeAll
  static void setup() {
    codec = new GeneralCodec(TEXT_BUFFER_SIZE, BINARY_BUFFER_SIZE, ERROR_BUFFER_SIZE, getLibPath());
  }

  @Test
  void testLibraryLoaded() {
    assertThat(codec, notNullValue());
    assertThat(codec.textBufferSize, equalTo(TEXT_BUFFER_SIZE));
    assertThat(codec.binaryBufferSize, equalTo(BINARY_BUFFER_SIZE));
    assertThat(codec.errorBufferSize, equalTo(ERROR_BUFFER_SIZE));
  }

  @Test
  void convertGeneral_uperToXer_messageFrame() {
    byte[] input = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER_14BITS);
    byte[] result = codec.convertGeneral(input, VEHICLE_EVENT_FLAGS_PDU, UPER, XER);
    assertThat(result, notNullValue());
    String xer = new String(result, StandardCharsets.UTF_8);
    assertThat(xer, equalTo(VEHICLE_EVENT_FLAGS_XER_14BITS));
  }

  @Test
  void convertGeneral_uperToJer_and_back_vehicleEventFlags() {
    byte[] input = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER_14BITS);
    log.debug("uper: {}", VEHICLE_EVENT_FLAGS_UPER_14BITS);
    // Ignore constraint check for this bitstring with extension
    byte[] jerBytes = codec.convertGeneral(input, VEHICLE_EVENT_FLAGS_PDU, UPER, JER, false);
    assertThat("jer is null", jerBytes, notNullValue());
    String jer = new String(jerBytes, StandardCharsets.UTF_8);
    log.debug("jer: {}", jer);
    byte[] roundTrip = codec.convertGeneral(jerBytes, VEHICLE_EVENT_FLAGS_PDU, JER, UPER, false);
    assertThat("round trip uper differs", hexFormat.formatHex(roundTrip),
        equalToIgnoringCase(VEHICLE_EVENT_FLAGS_UPER_14BITS));
  }

  @ParameterizedTest
  @MethodSource("ieee1609OerHex")
  void convertGeneral_oerToXer_and_back_ieee1609(final String oerHex) {
    log.debug("oer hex: {}", oerHex);
    byte[] oer = hexNoWs(oerHex);
    byte[] xerBytes = null;
    try {
      xerBytes = codec.convertGeneral(oer, IEEE_1609_PDU, OER, XER);
    } catch (Throwable e) {
      // Try again without constraint check in the event of error to log output
      xerBytes = codec.convertGeneral(oer, IEEE_1609_PDU, OER, XER, false);
      assertThat("xer is null", xerBytes, notNullValue());
      String xer = new String(xerBytes, StandardCharsets.UTF_8);
      log.debug("xer: {}", xer);
      throw e;
    }
    assertThat("xer is null", xerBytes, notNullValue());
    String xer = new String(xerBytes, StandardCharsets.UTF_8);
    log.debug("xer: {}", xer);
    assertThat(xer, containsString("<Ieee1609Dot2Data>"));

    byte[] roundTrip = codec.convertGeneral(xerBytes, IEEE_1609_PDU, XER, OER);
    assertThat("round trip oer differs", hexFormat.formatHex(roundTrip),
        equalToIgnoringCase(oerHex.replaceAll("\\s", "")));
  }

  @Test
  void xerToOer_oerToXer_explicitPdu() {
    byte[] oer = codec.xerToOer(IEEE_1609_PDU, UNSECURED_XER);
    assertThat(oer, notNullValue());
    String xer = codec.oerToXer(IEEE_1609_PDU, oer);
    assertThat(xer, containsString("<unsecuredData>0102030405</unsecuredData>"));
    byte[] roundTrip = codec.xerToOer(IEEE_1609_PDU, xer);
    assertThat(hexFormat.formatHex(roundTrip), equalToIgnoringCase(hexFormat.formatHex(oer)));
  }

  @Test
  void jerToOer_oerToJer_explicitPdu() {
    byte[] oer = codec.xerToOer(IEEE_1609_PDU, UNSECURED_XER);
    log.debug("oer: {}", hexFormat.formatHex(oer));
    String jer = codec.oerToJer(IEEE_1609_PDU, oer);
    assertThat(jer, notNullValue());
    log.debug("jer: {}", jer);
    byte[] roundTrip = codec.jerToOer(IEEE_1609_PDU, jer);
    assertThat(hexFormat.formatHex(roundTrip), equalToIgnoringCase(hexFormat.formatHex(oer)));
  }

  @Test
  void xerToUper_uperToXer_explicitPdu() {
    String xer = loadResource("SPAT_MF.xml");
    byte[] uper = codec.xerToUper(MessageFrameCodec.MESSAGE_FRAME_PDU, xer);
    assertThat(uper, notNullValue());
    String roundTripXer = codec.uperToXer(MessageFrameCodec.MESSAGE_FRAME_PDU, uper);
    byte[] roundTripUper = codec.xerToUper(MessageFrameCodec.MESSAGE_FRAME_PDU, roundTripXer);
    assertThat("round trip uper differs", hexFormat.formatHex(roundTripUper),
        equalToIgnoringCase(hexFormat.formatHex(uper)));
  }

  @Test
  void uperToJer_jerToUper_explicitPdu() {
    String xer = loadResource("SPAT_MF.xml");
    byte[] uper = codec.xerToUper(MessageFrameCodec.MESSAGE_FRAME_PDU, xer);
    log.debug("uper: {}", hexFormat.formatHex(uper));
    String jer = codec.uperToJer(MessageFrameCodec.MESSAGE_FRAME_PDU, uper);
    assertThat(jer, notNullValue());
    log.debug("jer: {}", jer);
    byte[] roundTrip = codec.jerToUper(MessageFrameCodec.MESSAGE_FRAME_PDU, jer);
    assertThat("round trip uper differs", hexFormat.formatHex(roundTrip),
        equalToIgnoringCase(hexFormat.formatHex(uper)));
  }

  @Test
  void convertGeneral_badPdu_throws() {
    byte[] input = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER_14BITS);
    assertThrows(
        RuntimeException.class,
        () -> codec.convertGeneral(input, "BadPDU", UPER, XER)
    );
  }

  @Test
  void convertBatch_convertsAllItems_uperToXer() {
    byte[] input = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER_14BITS);
    List<byte[]> results = codec.convertBatch(List.of(input, input), VEHICLE_EVENT_FLAGS_PDU, UPER, XER);
    assertThat(results, hasSize(2));
    for (byte[] result : results) {
      assertThat(new String(result, StandardCharsets.UTF_8), equalTo(VEHICLE_EVENT_FLAGS_XER_14BITS));
    }
  }

  @Test
  void convertBatch_skipsOversizedItem_returnsOnlySuccessful() {
    byte[] oversized = new byte[(int) BINARY_BUFFER_SIZE + 1];
    byte[] valid = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER_14BITS);
    List<byte[]> results = codec.convertBatch(List.of(oversized, valid), VEHICLE_EVENT_FLAGS_PDU, UPER, XER);
    assertThat(results, hasSize(1));
    assertThat(new String(results.getFirst(), StandardCharsets.UTF_8), equalTo(
        VEHICLE_EVENT_FLAGS_XER_14BITS));
  }

  @Test
  void convertBatch_skipsFailedItem_returnsOnlySuccessful() {
    byte[] malformed = hexFormat.parseHex(MALFORMED_SSM_UPER);
    byte[] valid = hexFormat.parseHex(loadResource("SSM.hex"));
    List<byte[]> results = codec.convertBatch(List.of(malformed, valid), SSM_PDU, UPER, XER);
    assertThat(results, hasSize(1));
  }

  @Test
  void invalidPduError() {
    byte[] inputBytes = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER_14BITS);
    assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, "BadPDU", UPER, XER);
        }
    );
  }

  @Test
  void invalidInputEncodingError() {
    byte[] inputBytes = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER_14BITS);
    assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, VEHICLE_EVENT_FLAGS_PDU, INVALID, XER);
        }
    );
  }

  @Test
  void invalidOutputEncodingError() {
    byte[] inputBytes = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER_14BITS);
    assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, VEHICLE_EVENT_FLAGS_PDU, UPER, INVALID);
        }
    );
  }

  @ParameterizedTest
  @MethodSource("convertData")
  void testConvertGeneral_XER(final String pdu, final String inputHex, final String expectXer) {
    byte[] inputBytes = hexFormat.parseHex(inputHex);
    byte[] result = codec.convertGeneral(inputBytes, pdu, UPER, XER);
    assertThat("result is null", result, notNullValue());
    if (expectXer != null) {
      String xer = new String(result, StandardCharsets.UTF_8);
      assertThat(xer, equalTo(expectXer));
    }
  }

  @ParameterizedTest
  @MethodSource("convertData_JER")
  void testConvertGeneral_JER(final String pdu, final String inputHex, final String expectJer) {
    byte[] inputBytes = hexFormat.parseHex(inputHex);
    byte[] result = codec.convertGeneral(inputBytes, pdu, UPER, JER);
    assertThat("result is null", result, notNullValue());
    if (expectJer != null) {
      String xer = new String(result, StandardCharsets.UTF_8);
      assertThat(xer, equalTo(expectJer));
    }
  }

  private static Stream<Arguments> convertData() {
    return Stream.of(
        Arguments.of(VEHICLE_EVENT_FLAGS_PDU, VEHICLE_EVENT_FLAGS_UPER_14BITS,
            VEHICLE_EVENT_FLAGS_XER_14BITS),
        Arguments.of(VEHICLE_EVENT_FLAGS_PDU, VEHICLE_EVENT_FLAGS_UPER_13BITS, VEHICLE_EVENT_FLAGS_XER_13BITS),
        Arguments.of(SSM_PDU, loadResource("SSM.hex"), null)
    );
  }

  private static Stream<Arguments> convertData_JER() {
    return Stream.of(
        Arguments.of(VEHICLE_EVENT_FLAGS_PDU, VEHICLE_EVENT_FLAGS_UPER_14BITS,
            VEHICLE_EVENT_FLAGS_JER_14BITS),
        Arguments.of(VEHICLE_EVENT_FLAGS_PDU, VEHICLE_EVENT_FLAGS_UPER_13BITS, VEHICLE_EVENT_FLAGS_JER_13BITS),
        Arguments.of(SSM_PDU, loadResource("SSM.hex"), null)
    );
  }

  private static Stream<String> ieee1609OerHex() {
    return Stream.of(
        loadResource("Ieee1609Dot2Data_unsecured_bsm.coer.hex"),
        loadResource("Ieee1609Dot2Data_signed.hex")
    );
  }


}
