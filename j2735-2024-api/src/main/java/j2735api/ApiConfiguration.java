package j2735api;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "j2735.api")
@Data
public class ApiConfiguration {
    long textBufferSize;
    long uperBufferSize;
    long messageFrameAllocateSize;
    long asnCodecCtxMaxStackSize;
}
