package com.cringebook.app.controllers;

import com.cringebook.app.config.StorageLocations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class FrontEnd {

    @Autowired
    private StorageLocations storage;

    @GetMapping("/")
    public ResponseEntity<Void> index() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/frontend/login.html"))
                .build();
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping(value = "/frontend/{filename:.+}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> getHtmlPage(@PathVariable String filename) throws IOException {
        Path file;
        try {
            file = storage.frontendFile(filename);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        if (!Files.isRegularFile(file)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        byte[] bytes = Files.readAllBytes(file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}