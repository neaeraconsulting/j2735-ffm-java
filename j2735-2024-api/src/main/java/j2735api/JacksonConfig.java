package j2735api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.Arrays;

// Config to enable application/octet-stream media type in POST apis
// Ref. https://gist.github.com/benoitdevos/fc49f3b9633eb7ba0f5c8dfe085cba14?permalink_comment_id=3081941#gistcomment-3081941
@Configuration
public class JacksonConfig {
    @Bean
    public MappingJackson2HttpMessageConverter jacksonConverter() {
        var converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(
          Arrays.asList(
                  MediaType.APPLICATION_JSON,
                  MediaType.APPLICATION_XML,
                  MediaType.TEXT_PLAIN,
                  MediaType.APPLICATION_OCTET_STREAM
          )
        );
        return converter;
    }
}
