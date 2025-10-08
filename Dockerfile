####################################################################################################
#
# Build container for shared library
#
FROM ubuntu:noble AS build-shared
USER root
WORKDIR /build

ADD ./asn1_codec/asn1c_combined/generated-files/2024.tar.gz /build
ADD ./CMakeLists.txt                                        /build
ADD ./run-lib.sh                                            /build

ENV CC=/usr/bin/clang

# Install prereqs
RUN apt update && \
    apt install -y clang && \
    apt install -y cmake && \
    cmake . && \
    cmake --build . --verbose && \
    mkdir out && \
    cp libasnapplication.so.1.0.0 out/libasnapplication.so

## Entrypoint for debugging
#ENTRYPOINT ["tail", "-f", "/dev/null"]
CMD ["/build/run-lib.sh"]

####################################################################################################
#
# Build container for jextract
#
FROM openjdk:22-jdk-slim AS jextract
USER root
WORKDIR /build

ADD ./j2735-2024-ffm-lib                                    /build/lib
COPY --from=build-shared ./build/generated-files/2024/*.h   /build/headers/
ADD ./run-jextract.sh                                       /build


ENV JEXTRACT_JAVA_OPTIONS="-Djava.library.path=/llvm/lib:/usr/lib64:/lib64:/lib:/usr/lib:/lib/x86_64-linux-gnu"
ENV JDK_HOME="/usr/local/openjdk-22"
ENV LLVM_HOME="/llvm"
ENV JEXTRACT="/jextract/jextract-22/bin/jextract"

RUN apt update && \
    apt install -y build-essential libncurses5 xz-utils wget && \
    mkdir /jextract && \
    wget -nc -O /jextract/jextract.tar.gz --show-progress https://download.java.net/java/early_access/jextract/22/6/openjdk-22-jextract+6-47_linux-x64_bin.tar.gz && \
    mkdir java-src && \
    cd /jextract && \
    tar -xzvf jextract.tar.gz && \
    chmod gu+x $JEXTRACT && \
    cd /build && \
    $JEXTRACT --include-dir /build/headers \
      --output /build/java-src \
      --target-package j2735_2024_MessageFrame \
      --library asnapplication \
      /build/headers/MessageFrame.h


## Entrypoint for debugging
#ENTRYPOINT ["tail", "-f", "/dev/null"]
CMD ["/build/run-jextract.sh"]

#########################################################################################
#
# Build container for Java app
#
FROM gradle:8.10-jdk22 AS builder
USER root
WORKDIR /home/app

# buildSrc
COPY ./buildSrc/src                     /home/app/buildSrc/src
COPY ./buildSrc/build.gradle            /home/app/buildSrc
COPY ./buildSrc/settings.gradle         /home/app/buildSrc

# api
COPY ./j2735-2024-api/src                       /home/app/j2735-2024-api/src
COPY ./j2735-2024-api/build.gradle              /home/app/j2735-2024-api

# lib
COPY ./j2735-2024-ffm-lib/src/main/java/j2735ffm              /home/app/j2735-2024-ffm-lib/src/main/java/j2735ffm
COPY --from=jextract /build/java-src/j2735_2024_MessageFrame  /home/app/j2735-2024-ffm-lib/src/main/java/j2735_2024_MessageFrame
COPY ./j2735-2024-ffm-lib/build.gradle                        /home/app/j2735-2024-ffm-lib

COPY ./settings.gradle                  /home/app
COPY ./gradle                           /home/app/gradle


RUN gradle clean build

## Entrypoint for debugging
ENTRYPOINT ["tail", "-f", "/dev/null"]

########################################################################################
#
# Run container
#
FROM eclipse-temurin:22-jdk-noble
WORKDIR /home

# Install native library
COPY --from=build-shared /build/out/libasnapplication.so /usr/lib

# Copy java app
COPY --from=builder /home/app/j2735-2024-api/build/libs/j2735-2024-api.jar /home

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/home/j2735-2024-api.jar"]

# Entrypoint for debugging
#ENTRYPOINT ["tail", "-f", "/dev/null"]