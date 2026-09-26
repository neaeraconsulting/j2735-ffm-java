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

import j2735ffm.GeneralCodec;
import j2735ffm.Ieee1609Dot2DataCodec;
import j2735ffm.MessageFrameCodec;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodecConfig {

    ApiConfiguration config;

    public CodecConfig(ApiConfiguration config) {
        this.config = config;
    }

    @Bean
    public MessageFrameCodec messageFrameCodec() {
      return new MessageFrameCodec(
          config.getTextBufferSize(),
          config.getBinaryBufferSize(),
          config.getErrorBufferSize(),
          libPath()
      );

    }

    @Bean
    public Ieee1609Dot2DataCodec dot2Codec() {
        return new Ieee1609Dot2DataCodec(
            config.getTextBufferSize(),
            config.getBinaryBufferSize(),
            config.getErrorBufferSize(),
            libPath()
        );
    }

    @Bean
    public GeneralCodec generalCodec() {
        return new GeneralCodec(
            config.getTextBufferSize(),
            config.getBinaryBufferSize(),
            config.getErrorBufferSize(),
            libPath()
        );
    }

    private Path libPath() {
        String libResource = System.getProperty("os.name").toLowerCase().contains("win")
            ? config.getWindowsLibraryPath()
            : config.getLibraryPath();
        return Paths.get(libResource);
    }

}
