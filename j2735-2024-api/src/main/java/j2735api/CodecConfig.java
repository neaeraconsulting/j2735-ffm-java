package j2735api;

import j2735ffm.MessageFrameCodec;
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
              config.getUperBufferSize(),
              Paths.get(config.getLibraryPath())
          );
    }

}
