#   Copyright 2025-2026 Neaera Consulting LLC
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

# Default to an optimized Release build when the caller doesn't choose a build
# type. Pass -DCMAKE_BUILD_TYPE=Debug (or set the BUILD_TYPE environment
# variable, as the Windows Docker build does) for friendly interactive
# debugging (unoptimized, full debug info); the PDB emission below happens
# either way. The environment variable fallback exists so the Windows build
# doesn't need to splice a -D flag into a PowerShell-reparsed command line.
if(NOT CMAKE_BUILD_TYPE)
    if(DEFINED ENV{BUILD_TYPE} AND NOT "$ENV{BUILD_TYPE}" STREQUAL "")
        set(CMAKE_BUILD_TYPE "$ENV{BUILD_TYPE}" CACHE STRING "Build type" FORCE)
    else()
        set(CMAKE_BUILD_TYPE Release CACHE STRING "Build type" FORCE)
    endif()
endif()

# Optimization level comes from CMAKE_BUILD_TYPE (CMAKE_C_FLAGS_RELEASE / _DEBUG)
# rather than being hardcoded here, so Debug actually builds unoptimized.
set(CMAKE_C_FLAGS "-DASN_PDU_COLLECTION -Wno-parentheses-equality")

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

# MinGW defaults to a "lib" prefix even for Windows DLLs, but System.loadLibrary("asnapplication")
# looks for "asnapplication.dll" (no prefix) on Windows. Drop the prefix there so the build
# output matches what the Java FFM bindings expect, and so convert-v2x.exe links against
# that same name without needing a renamed copy.
if(WIN32)
    set_target_properties(asnapplication PROPERTIES PREFIX "")
endif()

# CLI utility for exercising convert_bytes() from the command line
add_executable(convert-v2x
        src/main.c
)
target_link_libraries(convert-v2x PRIVATE asnapplication)

# Emit CodeView debug info (clang's -gcodeview, understood by WinDbg/Visual Studio,
# unlike the DWARF clang emits by default on non-Windows targets) and have lld
# collect it into a .pdb next to each binary, even for this optimized Release
# build, so crashes can be analyzed post-mortem in WinDbg.
if(WIN32)
    target_compile_options(asnapplication PRIVATE -g -gcodeview)
    target_compile_options(convert-v2x PRIVATE -g -gcodeview)
    target_link_options(asnapplication PRIVATE -Wl,--pdb=asnapplication.pdb)
    target_link_options(convert-v2x PRIVATE -Wl,--pdb=convert-v2x.pdb)
endif()