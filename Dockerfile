####################################################################################################
#
# Build container for shared library
#
FROM debian:trixie-slim AS build-shared
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
ENTRYPOINT ["tail", "-f", "/dev/null"]
#CMD ["/build/run-lib.sh"]

####################################################################################################
#
# Build container for jextract
#
FROM openjdk:22-jdk-slim AS jextract
USER root
WORKDIR /build

ADD ./j2735-2024-ffm-lib-build                              /build/lib
COPY --from=build-shared ./build/generated-files/2024/*.h   /build/headers/
ADD ./run-jextract.sh                                       /build

ENV JEXTRACT="/jextract/jextract-22/bin/jextract"

RUN apt update && \
    apt install -y build-essential libncurses5 wget && \
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

# api
COPY ./j2735-2024-api/src               /home/app/j2735-2024-api/src
COPY ./j2735-2024-api/build.gradle      /home/app/j2735-2024-api
COPY ./j2735-2024-api/settings.gradle   /home/app/j2735-2024-api
COPY ./j2735-2024-api/gradle            /home/app/j2735-2024-api/gradle

# lib
COPY ./j2735-2024-ffm-lib-build/src/main/java/j2735ffm        /home/app/j2735-2024-ffm-lib-build/src/main/java/j2735ffm
COPY --from=jextract /build/java-src/j2735_2024_MessageFrame  /home/app/j2735-2024-ffm-lib-build/src/main/java/j2735_2024_MessageFrame
COPY ./j2735-2024-ffm-lib-build/build.gradle                  /home/app/j2735-2024-ffm-lib-build
COPY ./j2735-2024-ffm-lib-build/settings.gradle               /home/app/j2735-2024-ffm-lib-build

ADD ./run-java.sh                       /home/app

RUN cd j2735-2024-api && gradle clean build

## Entrypoint for debugging
#ENTRYPOINT ["tail", "-f", "/dev/null"]
CMD ["/home/app/run-java.sh"]

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