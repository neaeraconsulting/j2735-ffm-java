package j2735api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import j2735ffm.MessageFrameCodec;

/**
 * HTTP Methods for converting J2735 MessageFrames between XER, JER and UPER
 * @author Ivan Yourshaw
 */
@RestController
public class J2735ApiController {

    MessageFrameCodec codec;

    @Autowired
    public J2735ApiController(MessageFrameCodec codec) {
        this.codec = codec;
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "I am in good health, thanks for asking.";
    }

    @PostMapping(
            value = "/xer/print",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String xerPrint(@RequestBody String xer)  {
        return codec.xerAsnFprint(xer);
    }

    @PostMapping(
            value = "/xer/uper/bin",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] xerToUper(@RequestBody String xer) {
        return codec.xerToUper(xer);
    }

    @PostMapping(
            value = "/jer/uper/bin",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] jerToUper(@RequestBody String jer) {
        return codec.jerToUper(jer);
    }

    @PostMapping(
            value = "/uper/xer",
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    public String uperToXer(@RequestBody byte[] uper) {
        return codec.uperToXer(uper);
    }

    @PostMapping(
            value = "/uper/jer",
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String uperToJer(@RequestBody byte[] uper) {
        return codec.uperToJer(uper);
    }

    @PostMapping(
            value = "/xer/jer",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String xerToJer(@RequestBody String xer) {
        return codec.xerToJer(xer);
    }

    @PostMapping(
            value = "/jer/xer",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    public String jerToXer(@RequestBody String jer) {
        return codec.jerToXer(jer);
    }
}
