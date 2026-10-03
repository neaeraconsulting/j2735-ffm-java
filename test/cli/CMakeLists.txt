#   Copyright 2026 Neaera Consulting LLC
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

# Black-box tests for the convert-v2x CLI: pipe a data file through the binary
# and compare stdout, exit code and stderr. Run `ctest --output-on-failure`
# from the build directory.
#
# add_cli_test(<name> STEPS <from:to>... INPUT <file> EXPECTED <file>
#              [EXIT <code>] [STDERR <regex>] [PDU <pdu>] [CRLF]
#              [LONG_LINE <length>])
function(add_cli_test name)
    cmake_parse_arguments(T "CRLF" "INPUT;EXPECTED;EXIT;STDERR;PDU;LONG_LINE" "STEPS" ${ARGN})
    if(NOT DEFINED T_EXIT)
        set(T_EXIT 0)
    endif()
    if(NOT DEFINED T_PDU)
        set(T_PDU MessageFrame)
    endif()
    list(JOIN T_STEPS "," steps)
    set(data ${CMAKE_CURRENT_SOURCE_DIR}/data)
    add_test(NAME cli_${name}
        COMMAND ${CMAKE_COMMAND}
            -DEXE=$<TARGET_FILE:convert-v2x>
            -DTEST_NAME=${name}
            -DPDU=${T_PDU}
            -DSTEPS=${steps}
            -DINPUT=${data}/${T_INPUT}
            -DCRLF=${T_CRLF}
            -DLONG_LINE=${T_LONG_LINE}
            -DEXPECTED=${data}/${T_EXPECTED}
            -DEXPECTED_EXIT=${T_EXIT}
            -DSTDERR_REGEX=${T_STDERR}
            -P ${CMAKE_CURRENT_SOURCE_DIR}/run_case.cmake
    )
endfunction()

# Valid input
add_cli_test(uper_to_jer
    STEPS uper:jer INPUT spat.uper.hex EXPECTED spat.jer)
add_cli_test(uper_to_jer_crlf
    STEPS uper:jer INPUT spat.uper.hex EXPECTED spat.jer CRLF)
add_cli_test(uper_to_jer_uppercase_hex
    STEPS uper:jer INPUT spat.uper.upper.hex EXPECTED spat.jer)

# Round trips back to the original hex
add_cli_test(uper_jer_uper_round_trip
    STEPS uper:jer jer:uper INPUT spat.uper.hex EXPECTED spat.uper.hex)
add_cli_test(uper_xer_oer_uper_round_trip
    STEPS uper:xer xer:oer oer:uper INPUT spat.uper.hex EXPECTED spat.uper.hex)

# Malformed hex fails the line: empty output line, reason on stderr, exit 1
add_cli_test(odd_length_hex
    STEPS uper:jer INPUT odd-length.hex EXPECTED blank-line.txt
    EXIT 1 STDERR "odd length 3")
add_cli_test(non_hex_character
    STEPS uper:jer INPUT non-hex.hex EXPECTED blank-line.txt
    EXIT 1 STDERR "non-hex character at offset 2")
add_cli_test(empty_hex_line
    STEPS oer:jer INPUT blank-line.txt EXPECTED blank-line.txt
    EXIT 1 STDERR "empty line" PDU Ieee1609Dot2Data)

# A failed line in a batch leaves an empty line in its place, and the other
# lines still convert
add_cli_test(batch_with_bad_line
    STEPS uper:jer INPUT batch.uper.hex EXPECTED batch.jer
    EXIT 1 STDERR "non-hex character at offset 0")

# A line longer than the 1 MiB - 1 line buffer fails as one record instead of
# being split into several, and the next line still converts
add_cli_test(overlong_line
    STEPS uper:jer INPUT spat.uper.hex EXPECTED failed-then-spat.jer
    LONG_LINE 1100000 EXIT 1 STDERR "exceeds maximum length of 1048575")

# A line of exactly the maximum length (counting a CR) is read whole: it fails
# as invalid hex of its full length, not for being too long
add_cli_test(max_length_line
    STEPS uper:jer INPUT spat.uper.hex EXPECTED failed-then-spat.jer
    LONG_LINE 1048575 EXIT 1 STDERR "odd length 1048575")
add_cli_test(max_length_line_crlf
    STEPS uper:jer INPUT spat.uper.hex EXPECTED failed-then-spat.jer
    LONG_LINE 1048574 CRLF EXIT 1 STDERR "non-hex character at offset 1048573")
