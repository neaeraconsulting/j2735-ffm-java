#!/bin/bash

# Echo on
set -x

# Copy the library Java code to the shared volume
cp -r /home/app/j2735-2024-ffm-lib/src/main/java/* /publish/src/main/java