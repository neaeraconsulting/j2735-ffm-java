package j2735api;

import j2735ffm.MessageFrameCodec;
import java.io.IOException;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class CodecConfig {

    ApiConfiguration config;

    public CodecConfig(ApiConfiguration config) {
        this.config = config;
    }

    @Bean
    public MessageFrameCodec messageFrameCodec() {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource libResource = resourceLoader.getResource("classpath:j2735ffm/libasnapplication.so");
      try {
        URI libUri = libResource.getURI();
          return new MessageFrameCodec(
              config.getTextBufferSize(),
              config.getUperBufferSize(),
              libUri
          );
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    }

}
