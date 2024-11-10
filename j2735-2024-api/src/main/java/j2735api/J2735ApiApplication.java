package j2735api;

import j2735ffm.MessageFrameCodec;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.InputStream;


@SpringBootApplication
public class J2735ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(j2735api.J2735ApiApplication.class, args);
    }

    @Bean
    public MessageFrameCodec messageFrameCodec() {
        return new MessageFrameCodec();
    }

//    // Ref. https://gist.github.com/benoitdevos/fc49f3b9633eb7ba0f5c8dfe085cba14?permalink_comment_id=2270442#gistcomment-2270442
//    // Support application/octet-stream content type
//    @Bean
//    public HttpMessageConverter addOctetStreamConverter() {
//        return new AbstractHttpMessageConverter<InputStream>(MediaType.APPLICATION_OCTET_STREAM) {
//            protected boolean supports(Class<?> clazz) {
//                return InputStream.class.isAssignableFrom(clazz);
//            }
//
//            protected InputStream readInternal(Class<? extends InputStream> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
//                return inputMessage.getBody();
//            }
//
//            protected void writeInternal(InputStream inputStream, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
//                IOUtils.copy(inputStream, outputMessage.getBody());
//            }
//        };
//    }
}
