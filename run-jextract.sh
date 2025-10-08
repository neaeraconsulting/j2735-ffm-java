#!/bin/bash

# Echo on
set -x

# Build jextract
#cd /jextract
#tar -xzvf jextract.tar.gz
#rm rm jextract.tar.gz
#chmod gu+x $JEXTRACT
#cd /build
#
#$JEXTRACT --include-dir /build/headers \
#  --output /build/java-src \
#  --target-package j2735_2024_MessageFrame \
#  --library asnapplication \
#  /build/headers/MessageFrame.h
#
#mkdir out
#cp -r /build/java-src/* out/

cp -r /build/java-src/* /publish/src/main/java

