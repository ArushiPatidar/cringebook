package com.cringebook.app.controllers;

import com.cringebook.app.entity.Episode;
import com.cringebook.app.entity.Memory;
import com.cringebook.app.repository.EpisodeRepo;
import com.cringebook.app.repository.MemoryRepo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
public class AddEpisode {

    @Autowired
    private EpisodeRepo episodeRepo;
    @Autowired
    private MemoryRepo memoryRepo;

    Authentication authentication = new Authentication();

    @GetMapping("/show episodes")
    public ResponseEntity<List<Episode>> getEpisode(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, Integer memoryId) {
        Integer user_id = authentication.getIdFromToken(jwtToken);
        if (user_id != 0) {
            Optional<Memory> episodeMemory = memoryRepo.findById(memoryId);
            if (episodeMemory.isPresent()) {
                Memory episodeMemory1 = episodeMemory.get();
                Integer UserIdEpisode = episodeMemory.get().getUserId();
                if (Objects.equals(UserIdEpisode, user_id)) {
                    try{
                        List<Episode> episodes= episodeRepo.findByMemoryId(memoryId);
                        return new ResponseEntity<>(episodes, HttpStatus.OK);
                    }catch (Exception e){
                        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        }return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @PostMapping("/save episode")
    public ResponseEntity<Integer> saveEpisode(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, String photo, String title, String description, Integer memoryId, @RequestParam(value = "image", required = false)MultipartFile image) throws IOException {
        Integer user_id = authentication.getIdFromToken(jwtToken);
        if (image != null && !image.isEmpty()){
            String uuid = UUID.randomUUID().toString();
            String filepath = "C:\\Users\\arushi\\Documents\\app\\app\\uploads\\" + uuid + image.getOriginalFilename();
            image.transferTo(new File(filepath));
            photo = uuid + image.getOriginalFilename();
        }
        if (user_id !=0){
            Optional<Memory> episodeMemory = memoryRepo.findById(memoryId);
            if (episodeMemory.isPresent()){
                Memory episodeMemory1 = episodeMemory.get();
                Integer UserIdEpisode = episodeMemory1.getUserId();
                if (Objects.equals(UserIdEpisode, user_id)){
                    Episode episode = new Episode(memoryId, title, photo, description);
                    try {
                        Episode insertedEpisode = episodeRepo.save(episode);
                        return new ResponseEntity<>(insertedEpisode.getEpisodeId(), HttpStatus.OK);
                    }catch (Exception e){
                        return new ResponseEntity<>(0, HttpStatus.FORBIDDEN);
                    }
                }
            }
        }
        return new ResponseEntity<>(0, HttpStatus.OK);
    }

    @PutMapping("/update episode")
    public ResponseEntity<Integer> updateEpisode(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, String photo, String title, String description, Integer episodeId){
        Integer user_id = authentication.getIdFromToken(jwtToken);
        if (user_id != 0){
            Optional<Episode> episode = episodeRepo.findById(episodeId);
            if (episode.isPresent()){
                Episode episode1 = episode.get();
                Optional<Memory> episodeOfMemory = memoryRepo.findById(episode1.getMemoryId());
                if (episodeOfMemory.isPresent()){
                    Memory episodeOfMemory1 = episodeOfMemory.get();
                    Integer UserIdEpisode = episodeOfMemory.get().getUserId();
                    if (Objects.equals(UserIdEpisode, user_id)){
                        if (description != null) episode1.setDescription(description);
                        if (title != null) episode1.setTitle(title);
                        if (photo != null) episode1.setPhoto(photo);
                        try{
                            episodeRepo.save(episode1);
                            return new ResponseEntity<>(1, HttpStatus.OK);
                        }catch (Exception e){
                            return new ResponseEntity<>(0, HttpStatus.BAD_REQUEST);
                        }
                    }
                }
            }
        }return new ResponseEntity<>(0, HttpStatus.NOT_FOUND);
    }



}
