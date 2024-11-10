package j2735api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import j2735ffm.MessageFrameCodec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * HTTP Methods for converting J2735 MessageFrames between XER, JER and UPER
 * @author Ivan Yourshaw
 */
@RestController
public class J2735ApiController {

    @GetMapping("/health")
    public String healthCheck() {
        return "I am in good health, thanks for asking.";
    }

    @PostMapping(
            value = "/xer/print",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String xerPrint(@RequestBody String xer)  {
        return MessageFrameCodec.xerAsnFprint(xer);
    }
}
