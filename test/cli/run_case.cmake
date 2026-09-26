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

# Runs one convert-v2x test case. Invoked by ctest as `cmake -P` with:
#
#   EXE            path to convert-v2x
#   TEST_NAME      name used for the temporary input file
#   PDU            PDU name passed to every step
#   STEPS          comma separated "from:to" encodings. More than one step pipes
#                  them together, e.g. "uper:jer,jer:uper" for a round trip.
#   INPUT          input file
#   CRLF           if true, feed the input with CRLF instead of LF line endings
#   LONG_LINE      if set, prepend one generated line of this many characters
#                  to the input, for lines too long to commit as data files.
#                  It is all "0"s except a final "z", so an even-length line
#                  read whole fails with "non-hex character at offset
#                  <LONG_LINE - 1>".
#   EXPECTED       file holding the expected stdout
#   EXPECTED_EXIT  exit code every step must return
#   STDERR_REGEX   optional regex that stderr must match
#
# Line endings are normalized to LF before comparing, so the same expected
# files work whether the test runs on Linux or Windows.

string(REPLACE "," ";" steps "${STEPS}")
set(commands)
foreach(step IN LISTS steps)
    string(REPLACE ":" ";" encodings "${step}")
    list(APPEND commands COMMAND "${EXE}" ${encodings} "${PDU}")
endforeach()

# Write the input out with exactly the line endings this case asks for,
# independent of how git checked out the data file.
file(READ "${INPUT}" input)
string(REPLACE "\r" "" input "${input}")
if(LONG_LINE)
    math(EXPR zeros "${LONG_LINE} - 1")
    string(REPEAT "0" ${zeros} long_line)
    string(PREPEND input "${long_line}z\n")
endif()
if(CRLF)
    string(REPLACE "\n" "\r\n" input "${input}")
endif()
set(input_file "${CMAKE_CURRENT_BINARY_DIR}/${TEST_NAME}.in")
file(WRITE "${input_file}" "${input}")

execute_process(${commands}
    INPUT_FILE "${input_file}"
    OUTPUT_VARIABLE actual
    ERROR_VARIABLE errors
    RESULTS_VARIABLE results
)

set(failures)
foreach(result IN LISTS results)
    if(NOT result EQUAL EXPECTED_EXIT)
        list(APPEND failures "exit code ${result}, expected ${EXPECTED_EXIT}")
        break()
    endif()
endforeach()

file(READ "${EXPECTED}" expected)
string(REPLACE "\r" "" expected "${expected}")
string(REPLACE "\r" "" actual "${actual}")
if(NOT actual STREQUAL expected)
    list(APPEND failures "stdout differs from ${EXPECTED}\n--- expected ---\n${expected}\n--- actual ---\n${actual}")
endif()

if(DEFINED STDERR_REGEX AND NOT STDERR_REGEX STREQUAL "" AND NOT errors MATCHES "${STDERR_REGEX}")
    list(APPEND failures "stderr does not match \"${STDERR_REGEX}\"")
endif()

if(failures)
    list(JOIN failures "\n" message)
    message(FATAL_ERROR "${message}\n--- stderr ---\n${errors}")
endif()
