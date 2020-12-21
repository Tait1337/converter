package de.clique.westwood.converter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
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

    private final static Logger LOGGER = LoggerFactory.getLogger(ConverterService.class);

    private final static long TTL_IN_HOURS = 24;
    private final static String TMP_PATH = System.getProperty("java.io.tmpdir");
    private final static String YOUTUBE_DL_PATH = "/usr/local/bin/youtube-dl";
    private final static String FFMPEG_PATH = "/usr/bin/ffmpeg";

    private static final Map<String, File> conversionQueue = new HashMap<>();
    private static final Map<String, String> conversionQueueStatus = new HashMap<>();

    private static LocalDateTime LAST_UPDATED = null;

    /**
     * Extract audio of given online video
     *
     * @param url the video url
     * @return the audio file (as mp3)
     */
    public String convertToMp3(URL url) {
        String ticket = UUID.randomUUID().toString();
        conversionQueue.put(ticket, null);
        conversionQueueStatus.put(ticket, "Please wait...");
        String[] options = {YOUTUBE_DL_PATH, "--extract-audio", "--audio-format mp3", "--audio-quality 192k", "--add-metadata"};

        new Thread(() -> {
            try {
                convert(ticket, url, options);
            } catch (Exception e) {
                LOGGER.error("Converter error.", e);
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
        conversionQueue.put(ticket, null);
        conversionQueueStatus.put(ticket, "Please wait...");
        String[] options = {YOUTUBE_DL_PATH, "--recode-video", "mp4", "--add-metadata"};

        new Thread(() -> {
            try {
                convert(ticket, url, options);
            } catch (Exception e) {
                LOGGER.error("Converter error.", e);
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
        return conversionQueue.get(ticket);
    }

    /**
     * Get the completion status for a conversion ticket
     *
     * @param ticket the conversion ticket
     * @return the completion status as text
     */
    public String getStatus(String ticket){
        return conversionQueueStatus.get(ticket);
    }

    /**
     * Convert a online video to target format
     *
     * @param ticket the conversion ticket
     * @param url     the video url
     * @param options the converter options
     * @throws IOException          on any execution error
     * @throws InterruptedException on any execution error
     */
    private void convert(String ticket, URL url, String[] options) throws IOException, InterruptedException {
        // update youtube and FFmpeg app when TTL is reached
        if (LAST_UPDATED == null || LAST_UPDATED.isBefore(LocalDateTime.now().minusHours(TTL_IN_HOURS))) {
            updateFFmpeg();
            updateYouTubeDownloader();
        }

        // start converting
        Path outputFile = Path.of(TMP_PATH + File.separatorChar + "%(title)s.%(ext)s");
        String[] defaultOptions = {"--ffmpeg-location " + FFMPEG_PATH, "-o " + "\"" + outputFile + "\"", url.toString()};
        String[] allOptions = new String[options.length + defaultOptions.length];
        System.arraycopy(options, 0, allOptions, 0, options.length);
        System.arraycopy(defaultOptions, 0, allOptions, options.length, defaultOptions.length);
        String cmd = String.join(" ", allOptions);
        Process ffmpegProcess = runCmd(cmd);

        // update converting ticket status
        FfmpegTicketStatusUpdater statusUpdateJob = new FfmpegTicketStatusUpdater(ticket, ffmpegProcess, conversionQueueStatus);
        statusUpdateJob.start();
        int exitCode = ffmpegProcess.waitFor();
        statusUpdateJob.interrupt();
        if (exitCode != 0) {
            conversionQueueStatus.put(ticket, "Error. Please try again.");
            throw new InterruptedException("Error while downloading and converting video file.");
        }

        // extract output filename
        try (var reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("[ffmpeg] Adding metadata to '")) {
                    outputFile = Path.of(line.substring("[ffmpeg] Adding metadata to '".length(), line.length() - 1));
                }
            }
        } catch (IOException e){
            conversionQueueStatus.put(ticket, "Error. Please try again.");
            throw new InterruptedException("Error while extracting output file.");
        }

        conversionQueue.put(ticket, outputFile.toFile());
        conversionQueueStatus.put(ticket, "Done");
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
        Process process = runCmd("curl -L https://yt-dl.org/downloads/latest/youtube-dl -o " + YOUTUBE_DL_PATH + " && chmod a+rx " + YOUTUBE_DL_PATH);
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
        LOGGER.debug("exec: {}", cmd);
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("/bin/bash", "-c", cmd);
        builder.directory(new File(TMP_PATH));
        builder.redirectErrorStream(true);
        return builder.start();
    }

}
