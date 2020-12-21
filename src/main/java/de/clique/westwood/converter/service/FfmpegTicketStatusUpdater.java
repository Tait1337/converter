package de.clique.westwood.converter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;

/**
 * Handles updates of the download ticket status accordingly to the ffmpeg process log.
 */
public class FfmpegTicketStatusUpdater extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(FfmpegTicketStatusUpdater.class);
    private static final String DOWNLOAD_TEXT = "[download]";
    private static final String DESTINATION_TEXT = "Destination: ";
    private static final String METADATA_TEXT = "[ffmpeg] Adding metadata to '";
    private final String ticket;
    private final Process process;
    private final Map<String, String> conversionQueueStatus;
    private Path outputFile;

    /**
     * Constructor
     * @param ticket the ticket to update
     * @param process running ffmpeg process
     * @param conversionQueueStatus conversion queue
     */
    public FfmpegTicketStatusUpdater(String ticket, Process process, Map<String, String> conversionQueueStatus) {
        this.ticket = ticket;
        this.process = process;
        this.conversionQueueStatus = conversionQueueStatus;
    }

    @Override
    public void run(){
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info(line);
                if (line.contains(DOWNLOAD_TEXT) && line.contains("%")){
                    // extract download progress
                    String downloadPercentage = line.substring(line.indexOf(DOWNLOAD_TEXT) + DOWNLOAD_TEXT.length(), line.indexOf("%"));
                    conversionQueueStatus.put(ticket, "Downloading..." + downloadPercentage + "%");
                } else if (line.contains(DESTINATION_TEXT) && !line.contains(DOWNLOAD_TEXT)){
                    // extract conversion progress
                    outputFile = Path.of(line.substring(line.indexOf(DESTINATION_TEXT) + DESTINATION_TEXT.length()));
                    long fileSizeInMB = 0;
                    while (process.isAlive()){
                        if (outputFile.toFile().exists()){
                            fileSizeInMB = outputFile.toFile().length() / 1024 / 1024;
                        }
                        conversionQueueStatus.put(ticket, "Converting..." + fileSizeInMB + "MB");
                        Thread.sleep(2500);
                    }
                } else if (line.startsWith(METADATA_TEXT)) {
                    // extract output filename
                    outputFile = Path.of(line.substring(METADATA_TEXT.length(), line.length() - 1));
                    conversionQueueStatus.put(ticket, "Done");
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Error while reading FFmpeg output", e);
            conversionQueueStatus.put(ticket, "Error. Please try again.");
        } catch (InterruptedException e) {
            LOGGER.warn("Thread was interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get the output of the ffmpeg conversion
     * @return the filename
     */
    public Path getOutputFile() {
        return outputFile;
    }

}
