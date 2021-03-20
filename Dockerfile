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
RUN apt-get update && apt-get install -y python

# install youtube-dl
RUN curl -L https://yt-dl.org/downloads/latest/youtube-dl -o /usr/local/bin/youtube-dl && chmod a+rx /usr/local/bin/youtube-dl

# install java
RUN curl https://download.java.net/java/GA/jdk16/7863447f0ab643c585b9bdebf67c69db/36/GPL/openjdk-16_linux-x64_bin.tar.gz | tar xz -C /usr/local/bin/
ENV JAVA_HOME=/usr/local/bin/jdk-16
ENV PATH $JAVA_HOME/bin:$PATH

# App
WORKDIR /usr/local/bin/converter
COPY ./target/converter-1.0.0-SNAPSHOT.jar ./app.jar
EXPOSE $PORT

ENTRYPOINT ["java", "-jar", "app.jar"]