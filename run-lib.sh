#!/bin/bash

# Echo on
set -x

# Copy the native library out to the shared volume
cp /build/out/* /publish/lib

