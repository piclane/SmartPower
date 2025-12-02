FROM public.ecr.aws/debian/debian:bookworm-slim AS build

# 必要なもののインストール
RUN apt-get update && \
    apt-get -y install curl java-common unzip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# amazon corretto のインストール
RUN if [ "$(arch)" = 'x86_64' ] ; then \
      CORRETTO_URL='https://corretto.aws/downloads/resources/21.0.9.11.1/java-21-amazon-corretto-jdk_21.0.9.11-1_amd64.deb' ; \
    elif [ "$(arch)" = 'arm64' ] || [ "$(arch)" = 'aarch64' ] ; then \
      CORRETTO_URL='https://corretto.aws/downloads/resources/21.0.9.11.1/java-21-amazon-corretto-jdk_21.0.9.11-1_arm64.deb' ; \
    fi ; \
    TEMP_DEB="$(mktemp)" && \
    curl -SsL -o "$TEMP_DEB" "$CORRETTO_URL" && \
    dpkg --install "$TEMP_DEB" ; \
    rm -f "$TEMP_DEB"

# fnm のインストール
RUN curl -fsSL https://fnm.vercel.app/install | bash -s -- --skip-shell --install-dir "/usr/local/bin"

# ビルド
COPY . /app
WORKDIR /app/frontend
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0
RUN fnm install --corepack-enabled
WORKDIR /app
ENV GRADLE_OPTS "-Dorg.gradle.daemon=false -Djdk.lang.Process.launchMechanism=vfork"
RUN eval "$(fnm env --shell bash)" && \
    ./gradlew clean && \
    ./gradlew bootJar

FROM public.ecr.aws/amazoncorretto/amazoncorretto:21.0.9-al2023-headless AS app

RUN mkdir /app
COPY --from=build /app/build/libs/*.jar /app

## その他
WORKDIR /app
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["java -jar *.jar"]
