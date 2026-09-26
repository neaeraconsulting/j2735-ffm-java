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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AsnEncodingTest {

  @ParameterizedTest
  @MethodSource("nameAndExpectedEncoding")
  void fromName_matchesCaseInsensitively(final String name, final AsnEncoding expected) {
    assertThat(AsnEncoding.fromName(name), equalTo(expected));
  }

  @Test
  void fromName_unknownName_throwsIllegalArgumentException() {
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> AsnEncoding.fromName("bogus")
    );
    assertThat(ex.getMessage(), containsString("Unknown encoding"));
  }

  @Test
  void uper_isSupportedAndBinary() {
    assertThat(AsnEncoding.UPER.isSupported(), equalTo(true));
    assertThat(AsnEncoding.UPER.isBinary(), equalTo(true));
  }

  @Test
  void xer_isSupportedButNotBinary() {
    assertThat(AsnEncoding.XER.isSupported(), equalTo(true));
    assertThat(AsnEncoding.XER.isBinary(), equalTo(false));
  }

  @Test
  void jer_isSupportedButNotBinary() {
    assertThat(AsnEncoding.JER.isSupported(), equalTo(true));
    assertThat(AsnEncoding.JER.isBinary(), equalTo(false));
  }

  @Test
  void invalid_isNotSupported() {
    assertThat(AsnEncoding.INVALID.isSupported(), equalTo(false));
  }

  private static Stream<Arguments> nameAndExpectedEncoding() {
    return Stream.of(
        Arguments.of("uper", AsnEncoding.UPER),
        Arguments.of("UPER", AsnEncoding.UPER),
        Arguments.of("Xer", AsnEncoding.XER),
        Arguments.of("oer", AsnEncoding.OER),
        Arguments.of("JER", AsnEncoding.JER),
        Arguments.of("invalid", AsnEncoding.INVALID)
    );
  }
}
