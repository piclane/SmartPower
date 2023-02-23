FROM node:18.14.2-buster-slim AS BUILD

# 必要なもののインストール
RUN apt-get update && \
    apt-get -y install wget java-common && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    corepack enable && \
    corepack prepare yarn@stable --activate

# amazon corretto のインストール
RUN if [ "$(arch)" = 'x86_64' ] ; then \
      CORRETTO_URL='https://corretto.aws/downloads/resources/17.0.6.10.1/java-17-amazon-corretto-jdk_17.0.6.10-1_amd64.deb' ; \
    elif [ "$(arch)" = 'arm64' ] || [ "$(arch)" = 'aarch64' ] ; then \
      CORRETTO_URL='https://corretto.aws/downloads/resources/17.0.6.10.1/java-17-amazon-corretto-jdk_17.0.6.10-1_arm64.deb' ; \
    fi ; \
    TEMP_DEB="$(mktemp)" && \
    wget -O "$TEMP_DEB" "$CORRETTO_URL" && \
    dpkg --install "$TEMP_DEB" ; \
    rm -f "$TEMP_DEB"

# ビルド
COPY . /build
RUN cd /build && \
    ./gradlew bootJar

FROM amazoncorretto:17.0.6-al2

RUN mkdir /app
COPY --from=BUILD /build/build/libs/*.jar /app

## その他
WORKDIR /app
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["java -jar *.jar"]
