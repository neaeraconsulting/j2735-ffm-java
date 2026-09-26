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

import j2735ffm.AsnEncoding;
import j2735ffm.GeneralCodec;
import j2735ffm.Ieee1609Dot2DataCodec;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import j2735ffm.MessageFrameCodec;

import java.io.IOException;
import java.util.Base64;
import java.util.HexFormat;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * HTTP Methods for converting J2735 MessageFrames and IEEE 1609.2 Data between XER, JER, UPER, and OER
 * @author Ivan Yourshaw
 */
@RestController
@Slf4j
public class ApiController {

    MessageFrameCodec codec;
    Ieee1609Dot2DataCodec dot2Codec;
    GeneralCodec generalCodec;

    @Autowired
    public ApiController(MessageFrameCodec codec, Ieee1609Dot2DataCodec dot2Codec,
        GeneralCodec generalCodec) {
        this.codec = codec;
        this.dot2Codec = dot2Codec;
        this.generalCodec = generalCodec;
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "I am in good health, thanks for checking.";
    }


    /**
     * Convert a J2735 MessageFrame in XER format to UPER format
     * @param xer The J2735 MessageFrame in XER format
     * @return The J2735 MessageFrame in UPER format as a byte array
     */
    @PostMapping(
            value = "/xer/uper/bin",
            consumes = APPLICATION_XML_VALUE,
            produces = APPLICATION_OCTET_STREAM_VALUE)
    public byte[] xerToUper(@RequestBody String xer) {
        return codec.xerToUper(xer);
    }



    /**
     * Convert a J2735 MessageFrame in XER format to UPER format
     * @param xer The J2735 MessageFrame in XER format
     * @return The J2735 MessageFrame in UPER format as a Base64 encoded string
     */
    @PostMapping(
            value = "/xer/uper/hex",
            consumes = APPLICATION_XML_VALUE,
            produces = TEXT_PLAIN_VALUE
    )
    public String xerToUperHex(@RequestBody String xer) {
        byte[] bytes = codec.xerToUper(xer);
        return HexFormat.of().formatHex(bytes);
    }




    /**
     * Convert a J2735 MessageFrame in UPER format to XER format
     * @param request The HttpServletRequest containing the J2735 MessageFrame in UPER format as a byte array
     * @return The J2735 MessageFrame in XER format
     */
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

    /**
     * Convert a J2735 MessageFrame in UPER format to XER format
     * @param uperHex The J2735 MessageFrame in UPER format as a Hex encoded string
     * @return The J2735 MessageFrame in XER format
     */
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
        value = "/oer/hex/xer",
        consumes = TEXT_PLAIN_VALUE,
        produces = APPLICATION_XML_VALUE
    )
    public String oerHexToXer(@RequestBody String oerHex) {
        byte[] bytes = HexFormat.of().parseHex(oerHex);
        return dot2Codec.oerToXer(bytes);
    }

    @PostMapping(
        value = "/xer/oer/hex",
        consumes = APPLICATION_XML_VALUE,
        produces = TEXT_PLAIN_VALUE
    )
    public String xerToOerHex(@RequestBody String xer) {
        byte[] bytes = dot2Codec.xerToOer(xer);
        return HexFormat.of().formatHex(bytes);
    }

    @PostMapping(
        value = "/xer/uper/hex/{pdu}",
        consumes = APPLICATION_XML_VALUE,
        produces = TEXT_PLAIN_VALUE
    )
    public String xerToUperHexAnyPdu(@RequestBody String xer, @PathVariable String pdu) {
        byte[] bytes = generalCodec.xerToUper(pdu, xer);
        return HexFormat.of().formatHex(bytes);
    }

    @PostMapping(
        value = "/uper/hex/xer/{pdu}",
        consumes = TEXT_PLAIN_VALUE,
        produces = APPLICATION_XML_VALUE
    )
    public String uperHexToXerAnyPdu(@RequestBody String uperHex, @PathVariable String pdu) {
        byte[] bytes = HexFormat.of().parseHex(uperHex);
        return generalCodec.uperToXer(pdu, bytes);
    }

    /**
     * Convert a J2735 MessageFrame in JER format to UPER format
     * @param jer The J2735 MessageFrame in JER format
     * @return The J2735 MessageFrame in UPER format as a Hex encoded string
     */
    @PostMapping(
        value = "/jer/uper/hex",
        consumes = APPLICATION_JSON_VALUE,
        produces = TEXT_PLAIN_VALUE
    )
    public String jerToUperHex(@RequestBody String jer) {
        byte[] bytes = codec.jerToUper(jer);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Convert a J2735 MessageFrame in UPER format to JER format
     * @param uperHex The J2735 MessageFrame in UPER format as a Hex encoded string
     * @return The J2735 MessageFrame in JER format
     */
    @PostMapping(
        value = "/uper/hex/jer",
        consumes = TEXT_PLAIN_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    public String uperHexToJer(@RequestBody String uperHex) {
        byte[] bytes = HexFormat.of().parseHex(uperHex);
        return codec.uperToJer(bytes);
    }

    /**
     * Convert an IEEE 1609.2 Data in JER format to OER format
     * @param jer The Ieee1609Dot2Data in JER format
     * @return The Ieee1609Dot2Data in OER format as a Hex encoded string
     */
    @PostMapping(
        value = "/jer/oer/hex",
        consumes = APPLICATION_JSON_VALUE,
        produces = TEXT_PLAIN_VALUE
    )
    public String jerToOerHex(@RequestBody String jer) {
        byte[] bytes = dot2Codec.jerToOer(jer);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Convert an IEEE 1609.2 Data in OER format to JER format
     * @param oerHex The Ieee1609Dot2Data in OER format as a Hex encoded string
     * @return The Ieee1609Dot2Data in JER format
     */
    @PostMapping(
        value = "/oer/hex/jer",
        consumes = TEXT_PLAIN_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    public String oerHexToJer(@RequestBody String oerHex) {
        byte[] bytes = HexFormat.of().parseHex(oerHex);
        return dot2Codec.oerToJer(bytes);
    }

    /**
     * Convert any PDU in JER format to UPER format
     * @param jer The PDU in JER format
     * @param pdu The name of the PDU, e.g. "MessageFrame"
     * @return The PDU in UPER format as a Hex encoded string
     */
    @PostMapping(
        value = "/jer/uper/hex/{pdu}",
        consumes = APPLICATION_JSON_VALUE,
        produces = TEXT_PLAIN_VALUE
    )
    public String jerToUperHexAnyPdu(@RequestBody String jer, @PathVariable String pdu) {
        byte[] bytes = generalCodec.jerToUper(pdu, jer);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Convert any PDU in UPER format to JER format
     * @param uperHex The PDU in UPER format as a Hex encoded string
     * @param pdu The name of the PDU, e.g. "MessageFrame"
     * @return The PDU in JER format
     */
    @PostMapping(
        value = "/uper/hex/jer/{pdu}",
        consumes = TEXT_PLAIN_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    public String uperHexToJerAnyPdu(@RequestBody String uperHex, @PathVariable String pdu) {
        byte[] bytes = HexFormat.of().parseHex(uperHex);
        return generalCodec.uperToJer(pdu, bytes);
    }

    /**
     * Batch convert line-delimited messages of any PDU between any encodings.
     * Binary encodings (UPER, OER) are Hex encoded, one message per line.
     * Text encodings (XER, JER) are one message per line.
     * Blank input lines are skipped.  If a message fails to convert, an empty line is
     * output in its place, so each output line corresponds to an input line.
     * @param body Line-delimited input messages
     * @param from The input encoding: xer, jer, uper, or oer
     * @param to The output encoding: xer, jer, uper, or oer
     * @param pdu The name of the PDU, e.g. "MessageFrame"
     * @return Line-delimited output messages
     */
    @PostMapping(
        value = "/batch/{from}/{to}/{pdu}",
        consumes = TEXT_PLAIN_VALUE,
        produces = TEXT_PLAIN_VALUE
    )
    public String batchConvert(@RequestBody String body, @PathVariable String from,
        @PathVariable String to, @PathVariable String pdu) {
        AsnEncoding fromEncoding = parseEncoding(from);
        AsnEncoding toEncoding = parseEncoding(to);
        return body.lines()
            .filter(line -> !line.isBlank())
            .map(line -> convertLine(line.strip(), pdu, fromEncoding, toEncoding))
            .collect(Collectors.joining("\n"));
    }

    // Convert one line of a batch, returning an empty string on failure
    private String convertLine(String line, String pdu, AsnEncoding fromEncoding,
        AsnEncoding toEncoding) {
        try {
            byte[] inputBytes = fromEncoding.isBinary()
                ? HexFormat.of().parseHex(line)
                : line.getBytes(StandardCharsets.UTF_8);
            byte[] outputBytes = generalCodec.convertGeneral(inputBytes, pdu, fromEncoding,
                toEncoding);
            return toEncoding.isBinary()
                ? HexFormat.of().formatHex(outputBytes)
                : new String(outputBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Error converting batch line: {}", e.getMessage());
            return "";
        }
    }

    private AsnEncoding parseEncoding(String name) {
        try {
            AsnEncoding encoding = AsnEncoding.fromName(name);
            if (encoding.isSupported()) {
                return encoding;
            }
        } catch (IllegalArgumentException e) {
            // fall through
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported encoding: " + name);
    }
}
