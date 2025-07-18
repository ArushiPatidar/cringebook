package com.cringebook.app.controllers;

import com.cringebook.app.entity.Episode;
import com.cringebook.app.entity.Friendship;
import com.cringebook.app.entity.Memory;
import com.cringebook.app.repository.FriendshipRepo;
import com.cringebook.app.repository.MemoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
public class AddMemory {

    @Autowired
    private MemoryRepo memoryRepo;

    @Autowired
    private FriendshipRepo friendshipRepo;

    Authentication authentication = new Authentication();

//    public Authentication getAuthentication() {
//        authentication.getIdFromToken();
//        return authentication;
//    }

    // Helper method to check if two users are friends
    private boolean areFriends(Integer userId1, Integer userId2) {
        if (userId1.equals(userId2)) {
            return false; // Same user, not a friendship
        }
        Friendship friendship = friendshipRepo.findFriendshipBetweenUsers(userId1, userId2);
        return friendship != null;
    }

    @GetMapping("/show_memory")
    public ResponseEntity<List<Memory>> getMemory(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, Integer userId) {
        Integer requesterId = authentication.getIdFromToken(jwtToken);
        if (requesterId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        // Default to requester's own memories if no userId specified
        Integer targetUserId = (userId != null) ? userId : requesterId;
        
        // Check access: owner can view own memories, friends can view friend's memories
        boolean canAccess = requesterId.equals(targetUserId) || areFriends(requesterId, targetUserId);
        
        if (canAccess) {
            try{
                List<Memory> memories = memoryRepo.findByUserId(targetUserId);
                return new ResponseEntity<>(memories, HttpStatus.OK);
            }catch (Exception e){
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
    }

    @PostMapping("/save memory")
    public ResponseEntity<Integer> saveMemory(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, String photo, String title, String description, MultipartFile image) throws IOException {
        Integer userId = authentication.getIdFromToken(jwtToken);
        if (image != null && !image.isEmpty()){
            String uuid = UUID.randomUUID().toString();
            String filepath = "C:\\Users\\arushi\\Documents\\app\\app\\uploads\\" + uuid + image.getOriginalFilename();
            image.transferTo(new File(filepath));
            photo = uuid + image.getOriginalFilename();
        }
        if (userId != 0) {
            Memory memory = new Memory(userId, photo, title, description);
            try {
                Memory InsertedMemory = memoryRepo.save(memory);
                return new ResponseEntity<>(InsertedMemory.getMemoryId(), HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>(0, HttpStatus.FORBIDDEN);
            }
        }
        return new ResponseEntity<>(0, HttpStatus.NOT_FOUND);
    }

    @PutMapping("/update memory")
    public ResponseEntity<Integer> updateMemory(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, Integer memoryId, String photo, String title, String description, MultipartFile image) throws IOException {
        Integer userId = authentication.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(0, HttpStatus.UNAUTHORIZED);
        }
        
        if (image != null && !image.isEmpty()){
            String uuid = UUID.randomUUID().toString();
            String filepath = "C:\\Users\\arushi\\Documents\\app\\app\\uploads\\" + uuid + image.getOriginalFilename();
            image.transferTo(new File(filepath));
            photo = uuid + image.getOriginalFilename();
        }
        Optional<Memory> memory = memoryRepo.findById(memoryId);
        if (memory.isPresent()){
            Memory memory1 = memory.get();
            // Only allow the owner to update their memory
            if (!memory1.getUserId().equals(userId)) {
                return new ResponseEntity<>(0, HttpStatus.FORBIDDEN);
            }
            if (description != null) memory1.setDescription(description);
            if (title != null) memory1.setTitle(title);
            if (photo != null) memory1.setPhoto(photo);
            try{
                memoryRepo.save(memory1);
                return new ResponseEntity<>(1, HttpStatus.OK);
            }catch (Exception e) {
                return new ResponseEntity<>(0, HttpStatus.BAD_REQUEST);
            }
        }else{
            return new ResponseEntity<>(0, HttpStatus.NO_CONTENT);
        }
    }

    @DeleteMapping("/delete memory")
    public ResponseEntity<Integer> deleteMemory(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, Integer memoryId){
        Integer userId = authentication.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(0, HttpStatus.UNAUTHORIZED);
        }
        
        Optional<Memory> memory = memoryRepo.findById(memoryId);
        if (memory.isPresent()){
            Memory memory1 = memory.get();
            // Only allow the owner to delete their memory
            if (!memory1.getUserId().equals(userId)) {
                return new ResponseEntity<>(0, HttpStatus.FORBIDDEN);
            }
            try{
                memoryRepo.deleteById(memoryId);
                return new ResponseEntity<>(1, HttpStatus.OK);
            }catch (Exception e){
                return new ResponseEntity<>(0, HttpStatus.BAD_REQUEST);
            }
        }else{
            return new ResponseEntity<>(0, HttpStatus.NO_CONTENT);
        }
    }
}
