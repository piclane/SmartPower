FROM public.ecr.aws/docker/library/node:18.19.0-bookworm-slim AS build

# 必要なもののインストール
RUN apt-get update && \
    apt-get -y install wget java-common && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    corepack enable && \
    corepack prepare yarn@stable --activate

# amazon corretto のインストール
RUN if [ "$(arch)" = 'x86_64' ] ; then \
      CORRETTO_URL='https://corretto.aws/downloads/resources/17.0.9.8.1/java-17-amazon-corretto-jdk_17.0.9.8-1_amd64.deb' ; \
    elif [ "$(arch)" = 'arm64' ] || [ "$(arch)" = 'aarch64' ] ; then \
      CORRETTO_URL='https://corretto.aws/downloads/resources/17.0.9.8.1/java-17-amazon-corretto-jdk_17.0.9.8-1_arm64.deb' ; \
    fi ; \
    TEMP_DEB="$(mktemp)" && \
    wget -O "$TEMP_DEB" "$CORRETTO_URL" && \
    dpkg --install "$TEMP_DEB" ; \
    rm -f "$TEMP_DEB"

# ビルド
COPY . /app
ENV GRADLE_OPTS "-Dorg.gradle.daemon=false -Djdk.lang.Process.launchMechanism=vfork"
WORKDIR /app
RUN ./gradlew clean
RUN ./gradlew bootJar && \
    ./gradlew bootJar --info

FROM public.ecr.aws/amazoncorretto/amazoncorretto:17.0.9-al2-native-headless

RUN mkdir /app
COPY --from=build /app/build/libs/*.jar /app

RUN yum install -y sudo shadow-utils prelink

## その他
WORKDIR /app
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["java -jar *.jar"]
