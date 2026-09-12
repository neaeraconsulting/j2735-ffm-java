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
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.notNullValue;

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
public class Ieee1609Dot2DataCodecTest {

  static final long TEXT_BUFFER_SIZE = 262144L;
  static final long BINARY_BUFFER_SIZE = 8192L;
  static final long ERROR_BUFFER_SIZE = 256L;

  static Ieee1609Dot2DataCodec codec;
  final static HexFormat hexFormat = HexFormat.of();

  private static final String UNSECURED_XER =
      "<Ieee1609Dot2Data><protocolVersion>3</protocolVersion><content>"
          + "<unsecuredData>0102030405</unsecuredData></content></Ieee1609Dot2Data>";

  static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }

  @BeforeAll
  public static void setup() {
    String libResource = isWindows() ? "j2735ffm/asnapplication.dll" : "j2735ffm/libasnapplication.so";
    URL url = Ieee1609Dot2DataCodecTest.class.getClassLoader().getResource(libResource);
    log.info("Loading library {}", libResource);
    if (url == null) {
      throw new RuntimeException("libasnapplication not found");
    }
    try {
      Path libPath = Paths.get(url.toURI());
      codec = new Ieee1609Dot2DataCodec(TEXT_BUFFER_SIZE, BINARY_BUFFER_SIZE, ERROR_BUFFER_SIZE, libPath);
      log.info("Created codec");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testLibraryLoaded() {
    assertThat(codec, notNullValue());
  }

  @Test
  public void oerToXer_unsecuredBsm() {
    String xer = codec.oerToXer(hexNoWs(loadResource("Ieee1609Dot2Data_unsecured_bsm.coer.hex")));
    assertThat(xer, notNullValue());
    assertThat(xer, containsString("<Ieee1609Dot2Data>"));
    assertThat(xer, containsString("<unsecuredData>"));
  }

  @Test
  public void oerToXer_signed() {
    String xer = codec.oerToXer(hexNoWs(loadResource("Ieee1609Dot2Data_signed.hex")));
    log.info("xer from oer: {}", xer);
    assertThat(xer, notNullValue());
    assertThat(xer, containsString("<signedData>"));
  }

  @ParameterizedTest
  @MethodSource("oerHexFixtures")
  public void oerToXer_xerToOer_roundTrip(final String oerHex) {
    byte[] oer = hexNoWs(oerHex);
    String xer = codec.oerToXer(oer);
    assertThat(xer, notNullValue());
    byte[] roundTrip = codec.xerToOer(xer);
    assertThat("round trip oer differs", hexFormat.formatHex(roundTrip),
        equalToIgnoringCase(oerHex.replaceAll("\\s", "")));
  }

  @Test
  public void xerToOer_fromSyntheticXer() {
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
