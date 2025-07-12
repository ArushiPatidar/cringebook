package com.cringebook.app.controllers;


import com.cringebook.app.entity.Episode;
import com.cringebook.app.entity.Interest;
import com.cringebook.app.entity.Photo;
import com.cringebook.app.repository.EpisodeRepo;
import com.cringebook.app.repository.MemoryRepo;
import com.cringebook.app.repository.PhotoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
public class AddPhoto {

    @Autowired
    private PhotoRepo photoRepo;

    @Autowired
    private EpisodeRepo episodeRepo;

    Authentication authentication = new Authentication();
    @GetMapping("/show_photos")
    public ResponseEntity<List<Photo>> getPhoto(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, Integer episodeId){
        System.out.println("episodeId " + episodeId );
        Integer user_id = authentication.getIdFromToken(jwtToken);
        Integer epiUserId = photoRepo.getUserIdForEpisodeId(episodeId);
        System.out.println("epiUserId " + epiUserId );
        System.out.println("user_id " + user_id );
        if (user_id !=0  && Objects.equals(epiUserId, user_id)){
            try{
                List<Photo> photos= photoRepo.findByEpisodeId(episodeId);
                return new ResponseEntity<>(photos, HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @GetMapping("/get_photo_from_url")
    public ResponseEntity<byte[]> getPhotoFromUrl(String url) throws IOException {

        System.out.println("path =  " + "C:\\Users\\arushi\\Documents\\app\\app\\uploads\\"+ url);
        File imgFile = new File("C:\\Users\\arushi\\Documents\\app\\app\\uploads\\" + url);

        byte[]  imageByte = Files.readAllBytes(imgFile.toPath());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        return new ResponseEntity<>(imageByte, headers, HttpStatus.OK);
    }

    @PostMapping("/save_photos")
    public ResponseEntity<Integer> savePhoto(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, String photo, Integer episodeId, MultipartFile image){
        if (image.isEmpty()){
            return new ResponseEntity<>(0, HttpStatus.NOT_FOUND);
        }
        try {
            String uuid = UUID.randomUUID().toString();
//        System.out.println(image.getName() + "  " + image.getSize() + "  " + image.getOriginalFilename());
            System.out.println(image.getName() + "  " + image.getSize() + "  " + uuid);
            System.out.println("episodeId " + episodeId);

            // saving uploaded file to directory

            String filepath = "C:\\Users\\arushi\\Documents\\app\\app\\uploads\\" + uuid + image.getOriginalFilename();
            image.transferTo(new File(filepath));


            Integer user_id = authentication.getIdFromToken(jwtToken);
            Integer epiUserId = photoRepo.getUserIdForEpisodeId(episodeId);
            if (user_id != 0 && Objects.equals(epiUserId, user_id)) {
                Photo photoObj = new Photo(episodeId, uuid + image.getOriginalFilename());
                try {
                    Photo insertedPhoto = photoRepo.save(photoObj);
                    return new ResponseEntity<>(insertedPhoto.getPhoto_id(), HttpStatus.OK);
                } catch (Exception e) {
                    return new ResponseEntity<>(0, HttpStatus.FORBIDDEN);
                }
            }
            return new ResponseEntity<>(0, HttpStatus.OK);
        }
        catch (Exception e){System.out.println(e.toString());}
        return new ResponseEntity<>(0, HttpStatus.OK);
    }
}
