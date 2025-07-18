package com.cringebook.app.controllers;

import com.cringebook.app.entity.User;
import com.cringebook.app.entity.Friendship;
import com.cringebook.app.entity.FriendRequest;
import com.cringebook.app.repository.UserRepo;
import com.cringebook.app.repository.FriendshipRepo;
import com.cringebook.app.repository.FriendRequestRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private FriendshipRepo friendshipRepo;
    
    @Autowired
    private FriendRequestRepo friendRequestRepo;
    
    @Autowired
    private Authentication authService;

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam String query,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken) {
        
        Integer currentUserId = authService.getIdFromToken(jwtToken);
        if (currentUserId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            List<User> users = userRepo.searchUsers(query);
            // Remove current user from results
            users.removeIf(user -> user.getUserID().equals(currentUserId));
            return new ResponseEntity<>(users, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @PathVariable Integer userId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken) {
        
        Integer currentUserId = authService.getIdFromToken(jwtToken);
        if (currentUserId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            User user = userRepo.findByUserId(userId);
            if (user == null) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
            
            Map<String, Object> response = new HashMap<>();
            
            // Check friendship status
            Friendship friendship = friendshipRepo.findFriendshipBetweenUsers(currentUserId, userId);
            boolean areFriends = friendship != null;
            
            // Check if there's a pending friend request
            FriendRequest pendingRequest = friendRequestRepo.findRequestBetweenUsers(currentUserId, userId);
            boolean hasPendingRequest = pendingRequest != null && "PENDING".equals(pendingRequest.getStatus());
            
            response.put("user", user);
            response.put("areFriends", areFriends);
            response.put("hasPendingRequest", hasPendingRequest);
            response.put("canSendRequest", !areFriends && !hasPendingRequest);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}