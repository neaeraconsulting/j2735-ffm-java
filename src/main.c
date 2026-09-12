/*
   Copyright 2025-2026 Neaera Consulting LLC

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
#include <stdio.h>
#include <sys/types.h>
#include <stdlib.h>    /* for atoi(3) */
#include <string.h>    /* for strerror(3) */
#include <ctype.h>     /* for isprint(3) */

#define EX_USAGE    64

#include <stddef.h>
#include "convert.h"
#include "../generated-files/2024/asn_application.h"

#define PDU_Type_Ptr    NULL

void usage() {
    printf("\nUsage:\n ./convert-v2x [from-encoding] [to-encoding] [PDU]\n\n");
    printf(" where 'from-encoding' and 'to-encoding' can be 'uper', 'oer', 'xer', or 'jer'.\n Reads one line of text from stdin\n Accepts UPER as hex encoded text.\n\n");
    printf("Examples:\n\n");
    printf("  Convert a file containing a hex encoded UPER MessageFrame to JER:\n");
    printf("  $ cat data.hex | ./convert-v2x uper jer MessageFrame > data.json\n\n");
    printf("  Convert a file containing a SPAT with no MessageFrame from canonical XER to JER:\n");
    printf("  $ cat data.xml | ./convert-v2x xer jer SPAT > data.json\n\n");
}

static void hex_to_bin(const char *hex, size_t hex_len, uint8_t *bytes) {
    size_t bin_len = hex_len / 2;
    for (unsigned int i = 0, j = 0; i < bin_len; i++, j+=2) {
        bytes[i] = (hex[j] % 32 + 9) % 25 * 16 + (hex[j+1] % 32 + 9) % 25;
    }
}

static void bin_to_hex(const uint8_t *bytes, size_t bytes_len, char *hex) {
    if (!bytes) {
        fprintf(stderr, "Null byte array passed to bin_to_hex\n");
        exit(EXIT_FAILURE);
    }
    for (unsigned int i = 0; i < bytes_len; i++) {
        hex += sprintf(hex, "%02x", bytes[i]);
    }
}

static enum asn_transfer_syntax abbrev_to_syntax(const char * abbrev) {
    if (!abbrev) {
        return ATS_INVALID;
    }
    if (strcmp("xer", abbrev) == 0) {
        return ATS_CANONICAL_XER;
    }
    if (strcmp("uper", abbrev) == 0) {
        return ATS_UNALIGNED_BASIC_PER;
    }
    if (strcmp("oer", abbrev) == 0) {
        return ATS_CANONICAL_OER;
    }
    return ATS_INVALID;
}

static int convert_str(const char * pdu_name,
    const char * from_encoding,
    const char * to_encoding,
    const char * ibuf,
    char * buf,
    const size_t max_buf_len) {

    const size_t len = strlen(ibuf);

    enum asn_transfer_syntax osyntax = abbrev_to_syntax(to_encoding);
    int is_to_uper = (osyntax == ATS_UNALIGNED_BASIC_PER);
    int is_to_oer = (osyntax == ATS_CANONICAL_OER);

    enum asn_transfer_syntax isyntax = abbrev_to_syntax(from_encoding);
    int is_from_uper = (isyntax == ATS_UNALIGNED_BASIC_PER);
    int is_from_oer = (isyntax == ATS_CANONICAL_OER);

    int num_encoded_bytes;

    uint8_t* obuf = calloc(max_buf_len, sizeof(uint8_t));

    const size_t err_buf_len = 255;
    char* err_buf = malloc(err_buf_len);
    const int check_constraints = 1;

    // If input is UPER or OER, convert from hex string to byte array
    if (is_from_uper || is_from_oer) {
        size_t input_bytes_len = len / 2;
        unsigned char bytes[input_bytes_len];
        hex_to_bin(ibuf, len, bytes);
        num_encoded_bytes = convert_bytes(pdu_name, from_encoding, to_encoding,
            bytes, input_bytes_len, obuf, max_buf_len,
            err_buf, err_buf_len, check_constraints);
    } else {
        num_encoded_bytes = convert_bytes(pdu_name, from_encoding, to_encoding,
            (const uint8_t*)ibuf, len, obuf, max_buf_len,
            err_buf, err_buf_len, check_constraints);
    }

    if (num_encoded_bytes < 0) {
      fprintf(stderr, "Codec returned an error: %.*s\n", (int)err_buf_len, err_buf);
      fprintf(stderr, "Dump of output buffer contents:");
      // Dump only printable characters in the output buffer.
      for (size_t i = 0; i < max_buf_len && isprint(obuf[i]); i++) {
          fputc(obuf[i], stderr);
      }
      fputc('\n', stderr);
      free(obuf);
      free(err_buf);
      return num_encoded_bytes;
    }


    // If output is UPER or OER, convert to hex string
    if (is_to_uper || is_to_oer) {
        // Convert UPER result to hex
        size_t hex_result_len = num_encoded_bytes * 2;
        char hex_result[hex_result_len];
        bin_to_hex(obuf, num_encoded_bytes, hex_result);
        if (hex_result_len > max_buf_len) {
            strncpy(buf, hex_result, max_buf_len);
            fprintf(stderr, "Warning truncating hex output.  Max buffer size %zu is too small\n", max_buf_len);
        } else {
            strncpy(buf, hex_result, hex_result_len);
        }
    } else {
        if (num_encoded_bytes > max_buf_len) {
            strncpy(buf, (const char *)obuf, max_buf_len);
            fprintf(stderr, "Warning, truncating output.  Max buffer size %zu is too small\n", max_buf_len);
        } else {
            strncpy(buf, (const char *)obuf, num_encoded_bytes);
        }
    }

    free(obuf);
    free(err_buf);

    return num_encoded_bytes;
}

int main(int ac, char *av[]) {

    if (!av[1] || !av[2] || !av[3]) {
        usage();
        exit(EX_USAGE);
    }

    printf("PDU=%s\n", av[3]);
    printf("from=%s\n", av[1]);
    printf("to=%s\n", av[2]);


    char line[2048];
    size_t size;
    if (fgets(line, sizeof(line), stdin) != NULL) {
        // Truncate after CR or LF
        size_t len = strlen(line);
        if (len > 0 && (line[len - 1] == '\n' || line[len - 1] == '\r')) {
            line[len - 1] = '\0';
        }
        printf("input: %s\n", line);
    }

    const size_t out_buf_size = 0xFFFFu;
    char * out_buf = calloc(out_buf_size, sizeof(uint8_t));
    convert_str(av[3], av[1], av[2], line, out_buf, out_buf_size);
    printf("%s\n", out_buf);
    free(out_buf);
}



