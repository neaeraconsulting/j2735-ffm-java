#!/bin/bash

# Echo on
set -x

# Copy the native library out to the shared volume
cp /build/out/* /build-lib
cp -r /build/generated-files/* /generated-files

# Copy the generated Java code to the shared volume
cp -r /build/java-src/* /generated-jextract

# Keep the container running
tail -f /dev/null