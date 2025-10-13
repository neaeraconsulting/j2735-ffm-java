#!/bin/bash

# Echo on
set -x

# Copy the generated Java code to the shared volume
cp -r /build/java-src/* /generated-jextract

