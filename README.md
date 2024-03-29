# converter
[![build status](https://github.com/Tait1337/converter/workflows/build/badge.svg)](https://github.com/Tait1337/converter/actions)
[![quality gate status](https://sonarcloud.io/api/project_badges/measure?project=Tait1337_converter&metric=alert_status)](https://sonarcloud.io/dashboard?id=Tait1337_converter)
[![license](https://img.shields.io/github/license/Tait1337/converter)](LICENSE)

Online YouTube2Mp3 converter. Download online videos as mp3 (audio) or mp4 (video).

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

Install Java JDK 17 or higher.

```
https://openjdk.java.net/install/index.html
```

Install Docker.
```
https://docs.docker.com/get-docker/
```

### Installing

Clone the Repository.

```
git clone https://github.com/tait1337/converter.git
```

Run the Web Application.
```
mvn spring-boot:run
```

Navigate to http://localhost:8080.

![Main Page](screenshot_index.png)

### Configuration

Within [application.properties](src/main/resources/application.properties) you can modify settings.

## Running the tests

No Tests exist.

## Deployment

The most basic option to run the Application is as Dockerimage with included Web Server and required convertion programs.

### Built and run as Dockerimage

```
./docker build -t converter:latest .
./docker run --env-file .env -p 8080:8080 -d converter:latest
```

## Contributing

I encourage all the developers out there to contribute to the repository and help me to update or expand it.

To contribute just create an issue together with the pull request that contains your features or fixes.

## Versioning

We use [GitHub](https://github.com/) for versioning. For the versions available, see the [tags on this repository](https://github.com/tait1337/converter/tags). 

## Authors

* **Oliver Tribess** - *Initial work* - [tait1337](https://github.com/tait1337)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

Please note that according to [§ 53 Absatz 1 UrhG](http://www.gesetze-im-internet.de/urhg/__53.html), creating copies is only allowed for private, non-commercial use. The regulations may differ in country and Streaming Platform.

## Acknowledgments

* [Youtube-dl](https://youtube-dl.org/) - the command-line program to download videos from [YouTube.com](https://www.youtube.com/) and a few [more sites](http://ytdl-org.github.io/youtube-dl/supportedsites.html)
* [FFmpeg](https://ffmpeg.org/) - the cross-platform solution to record, convert and stream audio and video
* Photographer of Background Photos from [Pexels](https://www.pexels.com/)
