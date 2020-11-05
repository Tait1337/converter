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
RUN curl https://download.java.net/java/GA/jdk15.0.1/51f4f36ad4ef43e39d0dfdbaf6549e32/9/GPL/openjdk-15.0.1_linux-x64_bin.tar.gz | tar xz -C /usr/local/bin/
ENV JAVA_HOME=/usr/local/bin/jdk-15.0.1
ENV PATH $JAVA_HOME/bin:$PATH

# App
WORKDIR /usr/local/bin/converter
COPY ./target/converter-1.0.0-SNAPSHOT.jar ./app.jar
EXPOSE $PORT

ENTRYPOINT ["java", "-jar", "app.jar"]