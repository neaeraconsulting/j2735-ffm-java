package j2735api;

import j2735ffm.MessageFrameCodec;
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
                config.getMessageFrameAllocateSize(),
                config.getAsnCodecCtxMaxStackSize()
        );
    }

}
