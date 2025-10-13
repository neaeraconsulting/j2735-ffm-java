#   Copyright 2025 Neaera Consulting LLC
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

cmake_minimum_required(VERSION 3.28)

# soversion.  bump this to indicate breaking changes in the shared library
set(LIB_SO_VERSION 1)
project(asnapplication LANGUAGES C VERSION ${LIB_SO_VERSION}.0.0 DESCRIPTION "shared library")
set(CMAKE_C_STANDARD 23)
set(CMAKE_C_FLAGS "-DASN_PDU_COLLECTION -Wno-parentheses-equality -O2")

file (GLOB generated_SRC
        "generated-files/2024/*.h"
        "generated-files/2024/*.c"
)

# Native shared library
add_library(asnapplication SHARED
        ${generated_SRC}
        src/convert.h
        src/convert.c
)

# Need this for includes with angle brackets to work
target_include_directories(asnapplication PUBLIC generated-files/2024)
target_include_directories(asnapplication PUBLIC src)

set_target_properties(asnapplication PROPERTIES VERSION ${PROJECT_VERSION} SOVERSION ${LIB_SO_VERSION})

# Sets -fPIC option to create library suitable for jextract
set_property(TARGET asnapplication PROPERTY POSITION_INDEPENDENT_CODE ON)

# Export all symbols for windows dll
set_property(TARGET asnapplication PROPERTY WINDOWS_EXPORT_ALL_SYMBOLS ON)