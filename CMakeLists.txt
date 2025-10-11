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

set_target_properties(asnapplication PROPERTIES VERSION ${PROJECT_VERSION} SOVERSION ${LIB_SO_VERSION})

# Sets -fPIC option to create library suitable for jextract
set_property(TARGET asnapplication PROPERTY POSITION_INDEPENDENT_CODE ON)