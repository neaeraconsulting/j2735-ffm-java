#!/bin/bash

# Echo on
set -x

# Copy the generated Java code to the shared volume
cp -r /build/java-src/* /j2735-2024-ffm-lib/src/main/java

