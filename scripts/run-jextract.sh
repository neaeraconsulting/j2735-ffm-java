#!/bin/bash

# Echo on
set -ex

# Copy the native library out to the shared volume
# The library name already includes architecture suffix (e.g., libasnapplication-arm64.so)
cp /build/out/*.so /build-lib/

# Copy generated files (architecture-independent)
cp -r /build/generated-files/* /generated-files

# Copy the generated Java code to the shared volume (architecture-independent)
cp -r /build/java-src/* /generated-jextract

# Keep the container running
tail -f /dev/null
