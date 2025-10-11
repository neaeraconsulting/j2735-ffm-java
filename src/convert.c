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
#include "/build/generated-files/2024/asn_application.h"
#include <stdlib.h>    /* for atoi(3) */
#include <string.h>    /* for strerror(3) */


#define PDU_Type_Ptr    NULL

#ifdef _WIN64
const void* nullptr = NULL;
#endif

extern asn_TYPE_descriptor_t *asn_pdu_collection[];


static enum asn_transfer_syntax abbrev_to_syntax(const char * abbrev) {
    if (strcmp("xer", abbrev) == 0) {
        return ATS_CANONICAL_XER;
    }
    if (strcmp("uper", abbrev) == 0) {
        return ATS_UNALIGNED_BASIC_PER;
    }
    fprintf(stderr, "Unknown encoding: %s  Expect 'xer' or 'uper'.\n", abbrev);
    exit(EXIT_FAILURE);
}




size_t convert_bytes(const char * pdu_name,
    const char * from_encoding,
    const char * to_encoding,
    const uint8_t * ibuf,
    size_t ibuf_len,
    uint8_t * obuf,
    size_t max_obuf_len) {

    asn_TYPE_descriptor_t *pduType = PDU_Type_Ptr;

    asn_TYPE_descriptor_t **pdu = asn_pdu_collection;
    while(*pdu && strcmp((*pdu)->name, pdu_name)) pdu++;
    if(*pdu) {
        pduType = *pdu;
    } else {
        fprintf(stderr, "%s: Unrecognized PDU.\n", pdu_name);
        exit(EXIT_FAILURE);
    }

    enum asn_transfer_syntax osyntax = abbrev_to_syntax(to_encoding);
    enum asn_transfer_syntax isyntax = abbrev_to_syntax(from_encoding);

    const asn_codec_ctx_t *opt_codec_ctx = nullptr;
    void *structure = nullptr;

    // Decode
    asn_dec_rval_t rval = asn_decode(opt_codec_ctx, isyntax, pduType, &structure, ibuf, ibuf_len);

    if (rval.code != RC_OK) {
        fprintf(stderr, "%s: Error decoding PDU\n", pduType->name);
        ASN_STRUCT_FREE(*pduType, structure);
        exit(EXIT_FAILURE);
    }

    // Check constraints
    char errbuff[256];
    size_t errlen = sizeof(errbuff);
    int constraint_result = asn_check_constraints(pduType, structure, errbuff, &errlen);
    if (constraint_result != 0) {
        fprintf(stderr, "Decoding was successful, but constraint check failed, can't re-encode: %s\n", errbuff);
        ASN_STRUCT_FREE(*pduType, structure);
        exit(EXIT_FAILURE);
    }

    // Encode
    asn_encode_to_new_buffer_result_t enc_result = {NULL, 0, nullptr};
    enc_result = asn_encode_to_new_buffer(opt_codec_ctx, osyntax, pduType, structure);
    if (!enc_result.buffer) {
        fprintf(stderr, "Error encoding to %d\n", osyntax);
        ASN_STRUCT_FREE(*pduType, structure);
        exit(EXIT_FAILURE);
    }
    ASN_STRUCT_FREE(*pduType, structure);

    const size_t num_encoded_bytes = enc_result.result.encoded;

    if (num_encoded_bytes > max_obuf_len) {
        memcpy(obuf, enc_result.buffer, max_obuf_len);
        fprintf(stderr, "Warning, truncating output.  Max buffer size %ld is too small\n", max_obuf_len);
    } else {
        memcpy(obuf, enc_result.buffer, num_encoded_bytes);
    }

    free(enc_result.buffer);
    return num_encoded_bytes;

}





