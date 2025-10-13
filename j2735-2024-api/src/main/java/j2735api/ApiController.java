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

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import j2735ffm.MessageFrameCodec;

import java.io.IOException;
import java.util.Base64;
import java.util.HexFormat;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * HTTP Methods for converting J2735 MessageFrames between XER, JER and UPER
 * @author Ivan Yourshaw
 */
@RestController
@Slf4j
public class ApiController {

    MessageFrameCodec codec;

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



}
