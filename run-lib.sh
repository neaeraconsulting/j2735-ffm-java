#!/bin/bash

# Echo on
set -x

## Build and test the CLI and shared library
#cmake .
#cmake --build . --verbose
#
#mkdir out
#cp libasn1application.so.1.0.0 out/

# Copy outputs to shared volume
cp /build/out/* /publish/lib

# Keep the container running
tail -f /dev/null