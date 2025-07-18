package com.cringebook.app.controllers;

import com.cringebook.app.entity.FriendRequest;
import com.cringebook.app.entity.Friendship;
import com.cringebook.app.entity.User;
import com.cringebook.app.repository.FriendRequestRepo;
import com.cringebook.app.repository.FriendshipRepo;
import com.cringebook.app.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    @Autowired
    private FriendRequestRepo friendRequestRepo;
    
    @Autowired
    private FriendshipRepo friendshipRepo;
    
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private Authentication authService;

    @PostMapping("/request")
    public ResponseEntity<String> sendFriendRequest(
            @RequestParam Integer recipientId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken) {
        
        Integer requesterId = authService.getIdFromToken(jwtToken);
        if (requesterId == 0) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        
        try {
            // Check if they're already friends
            Friendship existingFriendship = friendshipRepo.findFriendshipBetweenUsers(requesterId, recipientId);
            if (existingFriendship != null) {
                return new ResponseEntity<>("Already friends", HttpStatus.BAD_REQUEST);
            }
            
            // Check if request already exists
            FriendRequest existingRequest = friendRequestRepo.findRequestBetweenUsers(requesterId, recipientId);
            if (existingRequest != null && "PENDING".equals(existingRequest.getStatus())) {
                return new ResponseEntity<>("Request already pending", HttpStatus.BAD_REQUEST);
            }
            
            // Create new friend request
            FriendRequest friendRequest = new FriendRequest(requesterId, recipientId);
            friendRequestRepo.save(friendRequest);
            
            return new ResponseEntity<>("Friend request sent", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error sending request", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/accept")
    public ResponseEntity<String> acceptFriendRequest(
            @RequestParam Integer requestId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken) {
        
        Integer recipientId = authService.getIdFromToken(jwtToken);
        if (recipientId == 0) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        
        try {
            FriendRequest request = friendRequestRepo.findById(requestId).orElse(null);
            if (request == null || !request.getRecipientId().equals(recipientId)) {
                return new ResponseEntity<>("Request not found", HttpStatus.NOT_FOUND);
            }
            
            if (!"PENDING".equals(request.getStatus())) {
                return new ResponseEntity<>("Request already processed", HttpStatus.BAD_REQUEST);
            }
            
            // Update request status
            request.setStatus("ACCEPTED");
            friendRequestRepo.save(request);
            
            // Create friendship
            Friendship friendship = new Friendship(request.getRequesterId(), request.getRecipientId());
            friendshipRepo.save(friendship);
            
            return new ResponseEntity<>("Friend request accepted", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error accepting request", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<List<Map<String, Object>>> getPendingRequests(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken) {
        
        Integer userId = authService.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            List<FriendRequest> requests = friendRequestRepo.findPendingRequestsByRecipient(userId);
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (FriendRequest request : requests) {
                User requester = userRepo.findByUserId(request.getRequesterId());
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("requestId", request.getRequestId());
                requestData.put("requester", requester);
                requestData.put("createdAt", request.getCreatedAt());
                response.add(requestData);
            }
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<User>> getFriends(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken) {
        
        Integer userId = authService.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            List<Friendship> friendships = friendshipRepo.findFriendshipsByUser(userId);
            List<User> friends = new ArrayList<>();
            
            for (Friendship friendship : friendships) {
                Integer friendId = friendship.getUser1Id().equals(userId) ? 
                    friendship.getUser2Id() : friendship.getUser1Id();
                User friend = userRepo.findByUserId(friendId);
                if (friend != null) {
                    friends.add(friend);
                }
            }
            
            return new ResponseEntity<>(friends, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}