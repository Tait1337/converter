############################################################################
# Converter Web App served by ubuntu (932 MB)
#
# build from project root dir with: docker build -t converter:latest .
# run with: docker run --env-file .env -p 8080:8080 -d converter:latest
############################################################################
FROM ubuntu:latest
LABEL maintainer="tait1337"

# install ffmpeg
RUN apt-get update && apt-get install -y ffmpeg

# install curl
RUN apt-get update && apt-get install -y curl

# install python
RUN apt-get update && apt-get install -y python-is-python3

# install youtube-dl
RUN curl -L https://yt-dl.org/downloads/latest/youtube-dl -o /usr/local/bin/youtube-dl && chmod a+rx /usr/local/bin/youtube-dl

# install java
RUN curl -L https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.3%2B7/OpenJDK17U-jdk_x64_linux_hotspot_17.0.3_7.tar.gz | tar xz -C /usr/local/bin/
RUN mv /usr/local/bin/jdk-17.* /usr/local/bin/jdk-17
ENV JAVA_HOME=/usr/local/bin/jdk-17
ENV PATH $JAVA_HOME/bin:$PATH

# App
WORKDIR /usr/local/bin/converter
COPY ./target/converter-1.0.0-SNAPSHOT.jar ./app.jar
EXPOSE $PORT

ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]