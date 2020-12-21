package de.clique.westwood.converter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Handles updates of the download ticket status accordingly to the ffmpeg process log.
 */
public class FfmpegTicketStatusUpdater extends Thread {

    private final static Logger LOGGER = LoggerFactory.getLogger(FfmpegTicketStatusUpdater.class);
    private final String ticket;
    private final Process process;
    private final Map<String, String> convertionQueueStatus;

    /**
     * Constructor
     * @param ticket the ticket to update
     * @param process running ffmpeg process
     * @param conversionQueueStatus conversion queue
     */
    public FfmpegTicketStatusUpdater(String ticket, Process process, Map<String, String> conversionQueueStatus) {
        this.ticket = ticket;
        this.process = process;
        this.convertionQueueStatus = conversionQueueStatus;
    }

    @Override
    public void run(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = null;
        while (true) {
            try {
                if ((line = reader.readLine()) == null) break;
            } catch (IOException e) {
                LOGGER.warn("Error while reading FFmpeg output", e);
            }
            LOGGER.info(line);
            if (line.contains("[download]") && line.contains("%")){
                String downloadPercentageAsString = line.substring("[download]".length(), line.indexOf("%"));
                int downloadPercentage = (int) Double.parseDouble(downloadPercentageAsString);
                convertionQueueStatus.put(ticket, "Downloading..." + downloadPercentage + "%");
            } else if (line.contains("[ffmpeg] Destination: ")){
                String outputFileAsString = line.substring("[ffmpeg] Destination: ".length());
                File outputFile = new File(outputFileAsString);
                while (true){
                    long fileSizeInMB = 0;
                    if (outputFile.exists()){
                        fileSizeInMB = outputFile.length() / 1024 / 1024;
                    }
                    convertionQueueStatus.put(ticket, "Converting..." + fileSizeInMB + "MB");
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        LOGGER.warn("Thread was interrupted", e);
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

}
