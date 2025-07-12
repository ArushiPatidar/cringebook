package com.cringebook.app.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
//
//@RestController
//public class FrontEnd {
//    @GetMapping("/front_end")
//    public ResponseEntity<byte[]> getPhotoFromUrl( String url) throws IOException {
//
//        System.out.println("path =  " + "C:\\Users\\arushi\\Documents\\app\\app\\frontend\\"+ url);
//        File imgFile = new File("C:\\Users\\arushi\\Documents\\app\\app\\frontend\\" + url);
//
//        byte[]  imageByte = Files.readAllBytes(imgFile.toPath());
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.TEXT_HTML);
//
//        return new ResponseEntity<>(imageByte, headers, HttpStatus.OK);
//    }
//}

@RestController
public class FrontEnd {

    @GetMapping(value = "/frontend/{filename:.+}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> getHtmlPage(@PathVariable String filename) throws IOException {
        File file = new File("C:\\Users\\arushi\\Documents\\app\\app\\frontend\\" + filename);
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        byte[] bytes = Files.readAllBytes(file.toPath());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}