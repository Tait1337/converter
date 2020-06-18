package de.clique.westwood.converter.controller;

import de.clique.westwood.converter.service.ConverterService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Handles conversion of youtube videos.
 */
@RestController("/")
public class ConverterController {

    private ConverterService service;

    public ConverterController(ConverterService service) {
        this.service = service;
    }

    /**
     * Convert a given video (url) to mp3
     *
     * @param url the video url to convert
     * @return the conversion ticket
     */
    @PostMapping("/convertToMp3")
    public String convertToMp3(String url) throws MalformedURLException {
        return service.convertToMp3(new URL(url));
    }

    /**
     * Check the conversion status of a given ticket
     *
     * @param ticket the conversion ticket
     * @return true if convertion is complete, otherwise false
     */
    @GetMapping("/ticketStatus")
    public boolean ticketStatus(String ticket) {
        File outputFile = service.getFile(ticket);
        if (outputFile == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Download the converted file to a given ticket
     *
     * @param ticket the conversion ticket
     * @return the converted file
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(String ticket) throws FileNotFoundException {
        File outputFile = service.getFile(ticket);
        if (outputFile == null) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(outputFile));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputFile.getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(outputFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}
