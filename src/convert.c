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
#include "convert.h"
#include "../generated-files/2024/asn_application.h"
#include "../generated-files/2024/MessageFrame.h"
#include "../generated-files/2024/Ieee1609Dot2Data.h"
#include <limits.h>    /* for INT_MAX */
#include <stdlib.h>    /* for atoi(3) */
#include <string.h>    /* for strerror(3) */


#define PDU_Type_Ptr    NULL

extern asn_TYPE_descriptor_t *asn_pdu_collection[];

const int RETURN_ERROR = -1;

#ifdef ASN_ARENA
// Enough for the decoded structure of most messages.  A larger one spills to
// heap chunks, which asn_arena_reset frees.
#define ARENA_BUFFER_SIZE 65536
// The structure is in the arena of convert_bytes and goes with it, so the
// walk over it that frees each part is left out.
#define FREE_STRUCTURE(pdu_type, structure) ((void)(structure))
#else
#define FREE_STRUCTURE(pdu_type, structure) ASN_STRUCT_FREE(*(pdu_type), structure)
#endif

// Nearly every call is for one of two PDUs, so they are tried before the scan
// of the collection, which has over a thousand entries.
static asn_TYPE_descriptor_t * find_pdu(const char * pdu_name) {
    if (strcmp("MessageFrame", pdu_name) == 0) {
        return &asn_DEF_MessageFrame;
    }
    if (strcmp("Ieee1609Dot2Data", pdu_name) == 0) {
        return &asn_DEF_Ieee1609Dot2Data;
    }
    asn_TYPE_descriptor_t **pdu = asn_pdu_collection;
    while(*pdu && strcmp((*pdu)->name, pdu_name)) pdu++;
    return *pdu;
}

static enum asn_transfer_syntax abbrev_to_syntax(const char * abbrev, char * err_buf,
                                                size_t err_buf_len) {
    if (!abbrev) {
        snprintf(err_buf, err_buf_len, "Error: NULL encoding parameter\n");
        return ATS_INVALID;
    }
    if (strcmp("xer", abbrev) == 0) {
        return ATS_CANONICAL_XER;
    }
    if (strcmp("jer", abbrev) == 0) {
        return ATS_JER_MINIFIED;
    }
    if (strcmp("uper", abbrev) == 0) {
        return ATS_UNALIGNED_BASIC_PER;
    }
    if (strcmp("oer", abbrev) == 0) {
    	return ATS_CANONICAL_OER;
    }
    snprintf(err_buf, err_buf_len,
      "Unknown encoding: %s  Expect 'xer', 'jer', 'uper', or 'oer'.\n", abbrev);
    return ATS_INVALID;
}




static int convert(const char * pdu_name,
            const char * from_encoding,
            const char * to_encoding,
            const uint8_t * ibuf,
            size_t ibuf_len,
            uint8_t * obuf,
            size_t max_obuf_len,
            char * err_buf,
            size_t err_buf_len,
            int check_constraints) {

    asn_TYPE_descriptor_t *pduType = find_pdu(pdu_name);
    if (!pduType) {
        snprintf(err_buf, err_buf_len, "Unrecognized PDU: %s\n", pdu_name);
        return RETURN_ERROR;
    }

    enum asn_transfer_syntax osyntax = abbrev_to_syntax(to_encoding, err_buf, err_buf_len);
    if (osyntax == ATS_INVALID) {
        snprintf(err_buf, err_buf_len,
          "Unknown output encoding: %s  Expect 'xer', 'jer', 'uper', or 'oer'.\n", to_encoding);
        return RETURN_ERROR;
    }
    enum asn_transfer_syntax isyntax = abbrev_to_syntax(from_encoding, err_buf, err_buf_len);
    if (isyntax == ATS_INVALID) {
        snprintf(err_buf, err_buf_len,
          "Unknown input encoding: %s  Expect 'xer', 'jer', 'uper', or 'oer'.\n", from_encoding);
        return RETURN_ERROR;
    }

    const asn_codec_ctx_t *opt_codec_ctx = NULL;
    void *structure = NULL;

    // Decode
    asn_dec_rval_t rval = asn_decode(opt_codec_ctx, isyntax, pduType, &structure, ibuf, ibuf_len);

    if (rval.code != RC_OK) {
        FREE_STRUCTURE(pduType, structure);
        snprintf(err_buf, err_buf_len, "%s: Error decoding PDU\n", pduType->name);
        return RETURN_ERROR;
    }

    // Check constraints
    char errbuff[256];
    size_t errlen = sizeof(errbuff);
    if (check_constraints) {
      int constraint_result = asn_check_constraints(pduType, structure, errbuff, &errlen);
      if (constraint_result != 0) {
          snprintf(err_buf, err_buf_len,
            "Decoding was successful, but constraint check failed, can't re-encode: %s\n", errbuff);
          FREE_STRUCTURE(pduType, structure);
          return RETURN_ERROR;
      }
    }

    // Encode into the caller's buffer.  The encoder stops writing when the
    // buffer is full but keeps counting, so encoded is the whole size.
    asn_enc_rval_t enc_result =
        asn_encode_to_buffer(opt_codec_ctx, osyntax, pduType, structure, obuf, max_obuf_len);
    FREE_STRUCTURE(pduType, structure);

    // A failed encode returns encoded == -1, so check the count before
    // treating it as an unsigned length.
    if (enc_result.encoded < 0) {
        snprintf(err_buf, err_buf_len, "%s: Error encoding to %s\n", pduType->name, to_encoding);
        return RETURN_ERROR;
    }

    const size_t num_encoded_bytes = (size_t)enc_result.encoded;

    if (num_encoded_bytes > INT_MAX) {
        snprintf(err_buf, err_buf_len,
          "Error, output of %zu bytes is too large to return\n", num_encoded_bytes);
        return RETURN_ERROR;
    }

    if (num_encoded_bytes > max_obuf_len) {
        snprintf(err_buf, err_buf_len,
          "Error, truncating output.  Max buffer size %zu is too small\n", max_obuf_len);
        return RETURN_ERROR;
    }

    return (int)num_encoded_bytes;

}

int convert_bytes(const char * pdu_name,
            const char * from_encoding,
            const char * to_encoding,
            const uint8_t * ibuf,
            size_t ibuf_len,
            uint8_t * obuf,
            size_t max_obuf_len,
            char * err_buf,
            size_t err_buf_len,
            int check_constraints) {

#ifdef ASN_ARENA
    // Everything asn1c allocates for this call comes from an arena that starts
    // in a buffer on the stack, so each call has its own and threads don't
    // share one.
    char arena_buffer[ARENA_BUFFER_SIZE];
    asn_arena_t arena;
    asn_arena_init(&arena, arena_buffer, sizeof(arena_buffer));
    asn_arena_t *previous = asn_arena_use(&arena);
    const int result = convert(pdu_name, from_encoding, to_encoding, ibuf, ibuf_len,
                               obuf, max_obuf_len, err_buf, err_buf_len, check_constraints);
    asn_arena_use(previous);
    asn_arena_reset(&arena);
    return result;
#else
    return convert(pdu_name, from_encoding, to_encoding, ibuf, ibuf_len,
                   obuf, max_obuf_len, err_buf, err_buf_len, check_constraints);
#endif

}







