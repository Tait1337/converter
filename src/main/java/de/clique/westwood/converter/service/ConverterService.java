package de.clique.westwood.converter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    private static Map<String, File> convertionQueue = new HashMap();
    private static Map<String, String> convertionQueueStatus = new HashMap();

    /**
     * Extract audio of given online video
     *
     * @param url the video url
     * @return the audio file (as mp3)
     */
    public String convertToMp3(URL url) {
        String ticket = UUID.randomUUID().toString();
        convertionQueue.put(ticket, null);
        convertionQueueStatus.put(ticket, "Please wait...");
        String[] options = {youtube_dl_path, "--extract-audio", "--audio-format mp3", "--audio-quality 192k", "--add-metadata"};

        new Thread(() -> {
            try {
                convert(ticket, url, options);
            } catch (Exception e) {
                log.error("Convertion error.", e);
            }
        }).start();

        return ticket;
    }

    /**
     * Extract video of given online video
     *
     * @param url the video url
     * @return the video file (as mp4)
     */
    public String convertToMp4(URL url) {
        String ticket = UUID.randomUUID().toString();
        convertionQueue.put(ticket, null);
        convertionQueueStatus.put(ticket, "Please wait...");
        String[] options = {youtube_dl_path, "--recode-video", "mp4", "--add-metadata"};

        new Thread(() -> {
            try {
                convert(ticket, url, options);
            } catch (Exception e) {
                log.error("Convertion error.", e);
            }
        }).start();

        return ticket;
    }

    /**
     * Get the converted file that is related to the given ticket
     *
     * @param ticket the conversion ticket
     * @return the converted file or <code>null</code>
     */
    public File getFile(String ticket){
        return convertionQueue.get(ticket);
    }

    /**
     * Get the completion status for a conversion ticket
     *
     * @param ticket the conversion ticket
     * @return the completion status as text
     */
    public String getStatus(String ticket){
        return convertionQueueStatus.get(ticket);
    }

    /**
     * Convert a online video to target format
     *
     * @param ticket the conversion ticket
     * @param url     the video url
     * @param options the conerter options
     * @throws IOException          on any execution error
     * @throws InterruptedException on any execution error
     */
    private void convert(String ticket, URL url, String[] options) throws IOException, InterruptedException {
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

        // stream to frontend
        Thread logWatcher = new Thread(() ->
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while (true) {
                try {
                    if ((line = reader.readLine()) == null) break;
                } catch (IOException e) {
                    log.warn("Error while reading FFmpeg output", e);
                }
                log.info(line);
                if (line.contains("[download]") && line.contains("%")){
                    String downloadPercentageAsString = line.substring("[download]".length(), line.indexOf("%"));
                    int downloadPercentage = (int) Double.parseDouble(downloadPercentageAsString);
                    convertionQueueStatus.put(ticket, "Downloading..." + downloadPercentage + "%");
                } else if (line.contains("[ffmpeg] Destination: ")){
                    String outputfileAsString = line.substring("[ffmpeg] Destination: ".length());
                    File outputfile = new File(outputfileAsString);
                    while (true){
                        long fileSizeInMB = 0;
                        if (outputfile.exists()){
                            fileSizeInMB = outputfile.length() / 1024 / 1024;
                        }
                        convertionQueueStatus.put(ticket, "Converting..." + fileSizeInMB + "MB");
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {}
                    }
                }
            }
        });
        logWatcher.start();

        int exitCode = process.waitFor();
        logWatcher.interrupt();
        if (exitCode != 0) {
            convertionQueueStatus.put(ticket, "Error. Please try again.");
            throw new InterruptedException("Error while downloading and converting video file.");
        }

        // extract output filename
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("[ffmpeg] Adding metadata to '")) {
                    outputFile = Path.of(line.substring("[ffmpeg] Adding metadata to '".length(), line.length() - 1));
                }
            }
        }

        convertionQueue.put(ticket, outputFile.toFile());
        convertionQueueStatus.put(ticket, "Done");
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
        builder.redirectErrorStream(true);
        return builder.start();
    }

}
