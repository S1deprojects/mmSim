FROM ghcr.io/graalvm/graalvm-community:22 AS build

ARG GRADLE_VERSION=8.8

RUN microdnf install -y --nodocs unzip zlib && \
  microdnf clean all

RUN curl -L https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o gradle.zip && \
  mkdir -p /opt/gradle && \
  unzip -d /opt/gradle gradle.zip && \
  rm gradle.zip

ENV PATH="/opt/gradle/gradle-8.8/bin:${PATH}"

COPY . /build

RUN cd /build && \ 
  gradle nativeBuild

FROM debian AS runtime

COPY --from=build /build/build/native/nativeCompile/mmsim /usr/local/bin/mmsim

ENTRYPOINT [ "mmsim" ]
