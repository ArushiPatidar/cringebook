package com.cringebook.app.controllers;

import com.cringebook.app.entity.Episode;
import com.cringebook.app.entity.Friendship;
import com.cringebook.app.entity.Memory;
import com.cringebook.app.repository.EpisodeRepo;
import com.cringebook.app.repository.FriendshipRepo;
import com.cringebook.app.repository.MemoryRepo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
    @Autowired
    private FriendshipRepo friendshipRepo;

    Authentication authentication = new Authentication();

    // Helper method to check if two users are friends
    private boolean areFriends(Integer userId1, Integer userId2) {
        if (userId1.equals(userId2)) {
            return false; // Same user, not a friendship
        }
        Friendship friendship = friendshipRepo.findFriendshipBetweenUsers(userId1, userId2);
        return friendship != null;
    }

    @GetMapping("/show_episodes")
    public ResponseEntity<Map<String, Object>> getEpisode(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, @RequestParam("memoryId") Integer memoryId) {
        Integer requesterId = authentication.getIdFromToken(jwtToken);
        if (requesterId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        Optional<Memory> episodeMemory = memoryRepo.findById(memoryId);
        if (episodeMemory.isPresent()) {
            Memory memory = episodeMemory.get();
            Integer memoryOwnerId = memory.getUserId();
            
            // Check access: owner can view own episodes, friends can view friend's episodes
            boolean isOwner = requesterId.equals(memoryOwnerId);
            boolean canAccess = isOwner || areFriends(requesterId, memoryOwnerId);
            
            if (canAccess) {
                try{
                    List<Episode> episodes = episodeRepo.findByMemoryId(memoryId);
                    
                    // Return episodes with metadata
                    Map<String, Object> response = new HashMap<>();
                    response.put("episodes", episodes);
                    response.put("isOwner", isOwner);
                    response.put("memoryTitle", memory.getTitle());
                    
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }catch (Exception e){
                    return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }
        return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
    }

    @PostMapping("/save episode")
    public ResponseEntity<Integer> saveEpisode(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, String photo, String title, String description, Integer memoryId, @RequestParam(value = "image", required = false)MultipartFile image) throws IOException {
        Integer user_id = authentication.getIdFromToken(jwtToken);
        if (image != null && !image.isEmpty()){
            String uploadsDir = System.getProperty("user.dir") + "/uploads";
            java.io.File uploadDir = new java.io.File(uploadsDir);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            String uuid = UUID.randomUUID().toString();
            String filepath = uploadsDir + "/" + uuid + image.getOriginalFilename();
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
    public ResponseEntity<Integer> updateEpisode(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, String title, String description,String photo, Integer episodeId, MultipartFile image ) throws IOException {
        Integer user_id = authentication.getIdFromToken(jwtToken);
        if (image != null && !image.isEmpty()){
            String uploadsDir = System.getProperty("user.dir") + "/uploads";
            java.io.File uploadDir = new java.io.File(uploadsDir);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            String uuid = UUID.randomUUID().toString();
            String filepath = uploadsDir + "/" + uuid + image.getOriginalFilename();
            image.transferTo(new File(filepath));
            photo = uuid + image.getOriginalFilename();
        }
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

    @DeleteMapping("/delete_episode")
    public ResponseEntity<Integer> deleteEpisode(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, Integer episodeId){
        Integer userId = authentication.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(0, HttpStatus.UNAUTHORIZED);
        }
        
        Optional<Episode> episode = episodeRepo.findById(episodeId);
        if (episode.isPresent()){
            Episode episode1 = episode.get();
            Optional<Memory> episodeOfMemory = memoryRepo.findById(episode1.getMemoryId());
            if (episodeOfMemory.isPresent()) {
                Memory memory = episodeOfMemory.get();
                // Only allow the owner to delete their episode
                if (!memory.getUserId().equals(userId)) {
                    return new ResponseEntity<>(0, HttpStatus.FORBIDDEN);
                }
            } else {
                return new ResponseEntity<>(0, HttpStatus.NOT_FOUND);
            }
            try{
                episodeRepo.deleteById(episodeId);
                return new ResponseEntity<>(1, HttpStatus.OK);
            }catch (Exception e){
                return new ResponseEntity<>(0, HttpStatus.BAD_REQUEST);
            }
        }else{
            return new ResponseEntity<>(0, HttpStatus.NO_CONTENT);
        }
    }

}
