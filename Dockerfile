#########################################################################################
#
# Build container for Java app
#
FROM gradle:8.10-jdk22 AS builder
USER root
WORKDIR /home/app

# buildSrc
COPY ./buildSrc/src /home/app/buildSrc/src
COPY ./buildSrc/build.gradle /home/app/buildSrc
COPY ./buildSrc/settings.gradle /home/app/buildSrc

# api
COPY ./j2735-2024-api/src /home/app/j2735-2024-api/src
COPY ./j2735-2024-api/build.gradle /home/app/j2735-2024-api

# lib
COPY ./j2735-2024-ffm-lib/src /home/app/j2735-2024-ffm-lib/src
COPY ./j2735-2024-ffm-lib/build.gradle /home/app/j2735-2024-ffm-lib

COPY ./settings.gradle /home/app
COPY ./gradle /home/app/gradle

RUN cd /home/app && gradle clean build

## Entrypoint for debugging
#ENTRYPOINT ["tail", "-f", "/dev/null"]

########################################################################################
#
# Run container
#
FROM eclipse-temurin:22-jdk-noble
WORKDIR /home

# Install native library
COPY ./j2735-2024-ffm-lib/c-lib/libasnapplication.so /usr/lib

# Copy java app
COPY --from=builder /home/app/j2735-2024-api/build/libs/j2735-2024-api.jar /home

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/home/j2735-2024-api.jar"]

# Entrypoint for debugging
#ENTRYPOINT ["tail", "-f", "/dev/null"]