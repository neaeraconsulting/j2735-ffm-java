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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class CodecConfigTest {

  @Test
  void messageFrameCodec_selectsLibraryPathForCurrentOs() {
    CodecConfig codecConfig = new CodecConfig(newConfig());

    assertThrowsWithExpectedPath(codecConfig::messageFrameCodec);
  }

  @Test
  void dot2Codec_selectsLibraryPathForCurrentOs() {
    CodecConfig codecConfig = new CodecConfig(newConfig());

    assertThrowsWithExpectedPath(codecConfig::dot2Codec);
  }

  @Test
  void generalCodec_selectsLibraryPathForCurrentOs() {
    CodecConfig codecConfig = new CodecConfig(newConfig());

    assertThrowsWithExpectedPath(codecConfig::generalCodec);
  }

  private static ApiConfiguration newConfig() {
    ApiConfiguration config = new ApiConfiguration();
    config.setTextBufferSize(1024L);
    config.setBinaryBufferSize(1024L);
    config.setErrorBufferSize(256L);
    config.setLibraryPath("/nonexistent/linux/libasnapplication.so");
    config.setWindowsLibraryPath("C:/nonexistent/windows/asnapplication.dll");
    return config;
  }

  private static void assertThrowsWithExpectedPath(Executable executable) {
    RuntimeException ex = assertThrows(RuntimeException.class, executable);

    String expectedPath = isWindows()
        ? "C:/nonexistent/windows/asnapplication.dll"
        : "/nonexistent/linux/libasnapplication.so";
    assertThat(ex.getMessage(), containsString(Paths.get(expectedPath).toString()));
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }
}
