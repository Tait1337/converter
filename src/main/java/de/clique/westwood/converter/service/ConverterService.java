package de.clique.westwood.converter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Handles conversion of online videos with third party libs.
 */
@Service
public class ConverterService {

    private final long TTL_IN_HOURS = 24;
    private final String tmp_path = System.getProperty("java.io.tmpdir");
    private final String youtube_dl_path = "/usr/local/bin/youtube-dl";
    private final String ffmpeg_path = "/usr/bin/ffmpeg";

    private Logger log = LoggerFactory.getLogger(ConverterService.class);
    private LocalDateTime lastUpdated = null;

    /**
     * Extract audio of given online video
     *
     * @param url the video url
     * @return the audio file (as mp3)
     */
    public File convertToMp3(URL url) throws IOException, InterruptedException {
        String[] options = {youtube_dl_path, "--extract-audio", "--audio-format mp3", "--audio-quality 192k", "--add-metadata"};
        return convert(url, options);
    }

    /**
     * Convert a online video to target format
     *
     * @param url     the video url
     * @param options the conerter options
     * @return the converted file
     * @throws IOException          on any execution error
     * @throws InterruptedException on any execution error
     */
    private File convert(URL url, String[] options) throws IOException, InterruptedException {
        // update youtube and FFmpeg app when TTL is reached
        if (lastUpdated == null || lastUpdated.isBefore(LocalDateTime.now().minusHours(TTL_IN_HOURS))) {
            updateFFmpeg();
            updateYouTubeDownloader();
        }

        Path outputFile = Path.of(tmp_path + File.separatorChar + "%(title)s.%(ext)s");

        String[] defaultOptions = {"--ffmpeg-location " + ffmpeg_path, "-o " + "\"" + outputFile + "\"", url.toString()};
        String[] allOptions = new String[options.length + defaultOptions.length];
        System.arraycopy(options, 0, allOptions, 0, options.length);
        System.arraycopy(defaultOptions, 0, allOptions, options.length, defaultOptions.length);

        String cmd = String.join(" ", allOptions);
        Process process = runCmd(cmd);

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new InterruptedException("Error while downloading and converting video file.");
        }

        // extract output filename
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (line.startsWith("[ffmpeg] Destination: ")) {
                    outputFile = Path.of(line.substring("[ffmpeg] Destination: ".length()));
                }
            }
        }

        return outputFile.toFile();
    }

    /**
     * Update the FFmpeg app
     *
     * @throws IOException          on any execution error
     * @throws InterruptedException on any execution error
     * @see {@link} https://ffmpeg.org/
     */
    private void updateFFmpeg() throws IOException, InterruptedException {
        Process process = runCmd("apt-get update && apt-get install -y ffmpeg");
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new InterruptedException("Error while updating FFmpeg.");
        }
    }

    /**
     * Update the youtube-dl app
     *
     * @throws IOException          on any execution error
     * @throws InterruptedException on any execution error
     * @see {@link} https://yt-dl.org/
     */
    private void updateYouTubeDownloader() throws IOException, InterruptedException {
        Process process = runCmd("curl -L https://yt-dl.org/downloads/latest/youtube-dl -o " + youtube_dl_path + " && chmod a+rx " + youtube_dl_path);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new InterruptedException("Error while updating youtube dl.");
        }
    }

    /**
     * Execute a system command on /bin/bash
     *
     * @param cmd the commands to execute
     * @return the executing process
     * @throws IOException on any execution error
     */
    private Process runCmd(String cmd) throws IOException {
        log.debug("exec: " + cmd);
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("/bin/bash", "-c", cmd);
        builder.directory(new File(tmp_path));
        builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        return builder.start();
    }
}
