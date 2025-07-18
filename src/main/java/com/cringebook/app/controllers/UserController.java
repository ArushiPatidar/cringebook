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

    @GetMapping("/current-user-id")
    public ResponseEntity<Map<String, Object>> getCurrentUserId(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken) {
        
        Integer currentUserId = authService.getIdFromToken(jwtToken);
        if (currentUserId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", currentUserId);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getCurrentUserProfile(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken) {
        
        Integer currentUserId = authService.getIdFromToken(jwtToken);
        if (currentUserId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            User user = userRepo.findByUserId(currentUserId);
            if (user == null) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("isCurrentUser", true);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateCurrentUserProfile(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        
        Integer currentUserId = authService.getIdFromToken(jwtToken);
        if (currentUserId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            User user = userRepo.findByUserId(currentUserId);
            if (user == null) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
            
            // Update fields if provided
            if (name != null && !name.trim().isEmpty()) {
                user.setName(name.trim());
            }
            if (email != null && !email.trim().isEmpty()) {
                user.setEmail(email.trim());
            }
            if (phone != null && !phone.trim().isEmpty()) {
                user.setPhone(phone.trim());
            }
            
            userRepo.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("message", "Profile updated successfully");
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update profile");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> uploadProfilePicture(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken,
            @RequestParam("image") org.springframework.web.multipart.MultipartFile image) {
        
        Integer userId = authService.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        if (image.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "No image file provided");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
        
        try {
            // Generate unique filename
            String uuid = java.util.UUID.randomUUID().toString();
            String filename = uuid + "_profile_" + image.getOriginalFilename();
            String filepath = "C:\\Users\\arushi\\Documents\\app\\app\\uploads\\" + filename;
            
            // Save the file
            image.transferTo(new java.io.File(filepath));
            
            // Update user's profile picture in database
            User user = userRepo.findByUserId(userId);
            if (user != null) {
                user.setProfilePicture(filename);
                userRepo.save(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("profilePicture", filename);
                response.put("message", "Profile picture updated successfully");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to upload profile picture");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}