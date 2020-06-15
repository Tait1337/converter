package de.clique.westwood.converter.controller;

import de.clique.westwood.converter.service.ConverterService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
     * @return the result file
     */
    @PostMapping("/convertToMp3")
    public ResponseEntity<Resource> convertToMp3(String url) throws IOException, InterruptedException {
        File outputFile = service.convertToMp3(new URL(url));
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
