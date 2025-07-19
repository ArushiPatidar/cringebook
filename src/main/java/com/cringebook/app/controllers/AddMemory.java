package com.cringebook.app.controllers;

import com.cringebook.app.entity.Episode;
import com.cringebook.app.entity.Friendship;
import com.cringebook.app.entity.Memory;
import com.cringebook.app.entity.User;
import com.cringebook.app.repository.FriendshipRepo;
import com.cringebook.app.repository.MemoryRepo;
import com.cringebook.app.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

@RestController
public class AddMemory {

    @Autowired
    private MemoryRepo memoryRepo;

    @Autowired
    private FriendshipRepo friendshipRepo;

    @Autowired
    private UserRepo userRepo;

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
    public ResponseEntity<Map<String, Object>> getMemory(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, @RequestParam(value = "userId", required = false) Integer userId) {
        Integer requesterId = authentication.getIdFromToken(jwtToken);
        if (requesterId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        // Default to requester's own memories if no userId specified
        Integer targetUserId = (userId != null) ? userId : requesterId;
        
        // Check access: owner can view own memories, friends can view friend's memories
        boolean isOwner = requesterId.equals(targetUserId);
        boolean canAccess = isOwner || areFriends(requesterId, targetUserId);
        
        if (canAccess) {
            try{
                List<Memory> memories = memoryRepo.findByUserId(targetUserId);
                
                // Return memories with metadata
                Map<String, Object> response = new HashMap<>();
                response.put("memories", memories);
                response.put("isOwner", isOwner);
                
                return new ResponseEntity<>(response, HttpStatus.OK);
            }catch (Exception e){
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
    }

    @GetMapping("/feed")
    public ResponseEntity<Map<String, Object>> getFeed(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, 
                                                       @RequestParam(value = "page", defaultValue = "0") Integer page,
                                                       @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Integer userId = authentication.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            // Get user's own memories
            List<Memory> userMemories = memoryRepo.findByUserId(userId);
            
            // Get friends' memories
            List<Friendship> friendships = friendshipRepo.findFriendshipsByUser(userId);
            List<Memory> friendMemories = new ArrayList<>();
            
            for (Friendship friendship : friendships) {
                Integer friendId = friendship.getUser1Id().equals(userId) ? 
                    friendship.getUser2Id() : friendship.getUser1Id();
                List<Memory> friendUserMemories = memoryRepo.findByUserId(friendId);
                friendMemories.addAll(friendUserMemories);
            }
            
            // Combine and sort by creation date (newest first)
            List<Memory> allMemories = new ArrayList<>();
            allMemories.addAll(userMemories);
            allMemories.addAll(friendMemories);
            
            // Sort by memory ID descending (newer first) - assuming higher ID = newer
            allMemories.sort((m1, m2) -> m2.getMemoryId().compareTo(m1.getMemoryId()));
            
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, allMemories.size());
            List<Memory> pagedMemories = start < allMemories.size() ? 
                allMemories.subList(start, end) : new ArrayList<>();
            
            // Create enriched memory objects with user information
            List<Map<String, Object>> enrichedMemories = new ArrayList<>();
            for (Memory memory : pagedMemories) {
                User memoryOwner = userRepo.findByUserId(memory.getUserId());
                Map<String, Object> enrichedMemory = new HashMap<>();
                enrichedMemory.put("memoryId", memory.getMemoryId());
                enrichedMemory.put("userId", memory.getUserId());
                enrichedMemory.put("photo", memory.getPhoto());
                enrichedMemory.put("title", memory.getTitle());
                enrichedMemory.put("description", memory.getDescription());
                
                // Add user information
                if (memoryOwner != null) {
                    enrichedMemory.put("userName", memoryOwner.getUserName());
                    enrichedMemory.put("userFullName", memoryOwner.getName());
                    enrichedMemory.put("userProfilePicture", memoryOwner.getProfilePicture());
                } else {
                    enrichedMemory.put("userName", "unknown");
                    enrichedMemory.put("userFullName", "Unknown User");
                    enrichedMemory.put("userProfilePicture", null);
                }
                
                enrichedMemories.add(enrichedMemory);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("memories", enrichedMemories);
            response.put("currentPage", page);
            response.put("totalMemories", allMemories.size());
            response.put("hasMore", end < allMemories.size());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/save memory")
    public ResponseEntity<Integer> saveMemory(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, String photo, String title, String description, MultipartFile image) throws IOException {
        Integer userId = authentication.getIdFromToken(jwtToken);
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
