package j2735api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import j2735ffm.MessageFrameCodec;
import us.dot.its.jpo.ode.api.models.messages.TimestampedMessageFrame;
import us.dot.its.jpo.ode.api.models.messages.TimestampedMessageFrameList;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Base64;
import java.util.Formatter;
import java.util.HexFormat;

import static org.springframework.http.MediaType.*;

/**
 * HTTP Methods for converting J2735 MessageFrames between XER, JER and UPER
 * @author Ivan Yourshaw
 */
@RestController
@Slf4j
public class ApiController {

    MessageFrameCodec codec;
    private static final Base64.Decoder base64Decoder = Base64.getDecoder();

    @Autowired
    public ApiController(MessageFrameCodec codec) {
        this.codec = codec;
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "I am in good health, thanks for checking.";
    }

    @PostMapping(
            value = "/xer/uper/bin",
            consumes = APPLICATION_XML_VALUE,
            produces = APPLICATION_OCTET_STREAM_VALUE)
    public byte[] xerToUper(@RequestBody String xer) {
        return codec.xerToUper(xer);
    }

    @PostMapping(
            value = "/xer/uper/hex",
            consumes = APPLICATION_XML_VALUE,
            produces = TEXT_PLAIN_VALUE
    )
    public String xerToUperHex(@RequestBody String xer) {
        byte[] bytes = codec.xerToUper(xer);
        return HexFormat.of().formatHex(bytes);
    }

    @PostMapping(
            value = "/jer/uper/bin",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_OCTET_STREAM_VALUE)
    public byte[] jerToUper(@RequestBody String jer) {
        return codec.jerToUper(jer);
    }

    @PostMapping(
            value = "jer/uper/hex",
            consumes = APPLICATION_JSON_VALUE,
            produces = TEXT_PLAIN_VALUE
    )
    public String jerToUperHex(@RequestBody String jer) {
        byte[] bytes = codec.jerToUper(jer);
        return HexFormat.of().formatHex(bytes);
    }

    @PostMapping(
            value = "/uper/bin/xer",
            consumes = APPLICATION_OCTET_STREAM_VALUE,
            produces = APPLICATION_XML_VALUE
    )
    public String uperToXer(HttpServletRequest request) {
        try (var is = request.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            log.info("Read {} bytes", bytes.length);
            return codec.uperToXer(bytes);
        } catch (IOException ioe) {
            return ioe.getMessage();
        }
    }

    @PostMapping(
            value = "/uper/hex/xer",
            consumes = TEXT_PLAIN_VALUE,
            produces = APPLICATION_XML_VALUE
    )
    public String uperHexToXer(@RequestBody String uperHex) {
        byte[] bytes = HexFormat.of().parseHex(uperHex);
        return codec.uperToXer(bytes);
    }

    @PostMapping(
            value = "uper/b64/xer",
            consumes = TEXT_PLAIN_VALUE,
            produces = APPLICATION_XML_VALUE
    )
    public String uperBase64ToXer(@RequestBody String base64) {
        byte[] bytes = base64Decoder.decode(base64);
        return codec.uperToXer(bytes);
    }

    @PostMapping(
            value = "/uper/bin/jer",
            consumes = APPLICATION_OCTET_STREAM_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public String uperToJer(HttpServletRequest request) {
        try (var is = request.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            log.info("Read {} bytes", bytes.length);
            return codec.uperToJer(bytes);
        } catch (IOException ioe) {
            return ioe.getMessage();
        }
    }

    @PostMapping(
            value = "/uper/hex/jer",
            consumes = TEXT_PLAIN_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public String uperHexToJer(@RequestBody String uperHex) {
        byte[] bytes = HexFormat.of().parseHex(uperHex);
        return codec.uperToJer(bytes);
    }

    @PostMapping(
            value = "/uper/b64/jer",
            consumes = TEXT_PLAIN_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public String uperBase64ToJer(@RequestBody String base64) {
        byte[] bytes = base64Decoder.decode(base64);
        return codec.uperToJer(bytes);
    }

    @PostMapping(
            value = "/xer/jer",
            consumes = APPLICATION_XML_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public String xerToJer(@RequestBody String xer) {
        return codec.xerToJer(xer);
    }

    @PostMapping(
            value = "/jer/xer",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_XML_VALUE
    )
    public String jerToXer(@RequestBody String jer) {
        return codec.jerToXer(jer);
    }

    @PostMapping(
            value = "/batch",
            consumes = APPLICATION_JSON_VALUE,
            produces = TEXT_PLAIN_VALUE
    )
    public String batch(@RequestBody TimestampedMessageFrameList messageFrameList) {
        log.info("Start decoding batch with {} items", messageFrameList.size());
        Formatter xmlList = new Formatter();
        try (var arena = Arena.ofConfined()) {
            MemorySegment messageFrameMemory = arena.allocate(codec.messageFrameAllocateSize);
            MemorySegment outputBuffer = arena.allocate(codec.textBufferSize);
            for (TimestampedMessageFrame messageFrame : messageFrameList) {
                String xer = codec.uperToXer(messageFrame.getMessageFrame(), arena, messageFrameMemory, codec.textBufferSize, outputBuffer);
                // Line-delimited XML
                xmlList.format("%s%n", xer);
            }
        }
        log.info("Finished decoding batch");
        return xmlList.toString();
    }
}
