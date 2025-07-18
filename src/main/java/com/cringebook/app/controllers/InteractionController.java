package com.cringebook.app.controllers;

import com.cringebook.app.entity.Comment;
import com.cringebook.app.entity.Like;
import com.cringebook.app.entity.Friendship;
import com.cringebook.app.repository.CommentRepo;
import com.cringebook.app.repository.LikeRepo;
import com.cringebook.app.repository.FriendshipRepo;
import com.cringebook.app.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class InteractionController {

    @Autowired
    private LikeRepo likeRepo;

    @Autowired
    private CommentRepo commentRepo;

    @Autowired
    private FriendshipRepo friendshipRepo;

    @Autowired
    private UserRepo userRepo;

    Authentication authentication = new Authentication();

    // Helper method to check if two users are friends
    private boolean areFriends(Integer userId1, Integer userId2) {
        if (userId1.equals(userId2)) {
            return false; // Same user, not a friendship
        }
        Friendship friendship = friendshipRepo.findFriendshipBetweenUsers(userId1, userId2);
        return friendship != null;
    }

    @PostMapping("/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken,
                                                          @RequestParam("targetType") String targetType,
                                                          @RequestParam("targetId") Integer targetId) {
        Integer userId = authentication.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        try {
            // Check if user already liked this target
            Like existingLike = likeRepo.findByUserAndTarget(userId, targetType, targetId);
            
            Map<String, Object> response = new HashMap<>();
            
            if (existingLike != null) {
                // Unlike - remove the like
                likeRepo.delete(existingLike);
                response.put("liked", false);
                response.put("message", "Unliked");
            } else {
                // Like - add the like
                Like newLike = new Like(userId, targetType, targetId);
                likeRepo.save(newLike);
                response.put("liked", true);
                response.put("message", "Liked");
            }
            
            // Get updated count
            Integer likeCount = likeRepo.countLikesForTarget(targetType, targetId);
            response.put("likeCount", likeCount);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/likes")
    public ResponseEntity<Map<String, Object>> getLikes(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken,
                                                        @RequestParam("targetType") String targetType,
                                                        @RequestParam("targetId") Integer targetId) {
        Integer userId = authentication.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        try {
            Integer likeCount = likeRepo.countLikesForTarget(targetType, targetId);
            Like userLike = likeRepo.findByUserAndTarget(userId, targetType, targetId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("likeCount", likeCount);
            response.put("userLiked", userLike != null);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/comment")
    public ResponseEntity<Map<String, Object>> addComment(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken,
                                                          @RequestParam("targetType") String targetType,
                                                          @RequestParam("targetId") Integer targetId,
                                                          @RequestParam("commentText") String commentText) {
        Integer userId = authentication.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        if (commentText == null || commentText.trim().isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        try {
            // TODO: Add friendship check for commenting - only friends can comment
            // For now, allowing all authenticated users to comment
            
            Comment newComment = new Comment(userId, targetType, targetId, commentText.trim());
            Comment savedComment = commentRepo.save(newComment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("comment", savedComment);
            response.put("message", "Comment added");
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/comments")
    public ResponseEntity<Map<String, Object>> getComments(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken,
                                                           @RequestParam("targetType") String targetType,
                                                           @RequestParam("targetId") Integer targetId) {
        Integer userId = authentication.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        try {
            List<Comment> comments = commentRepo.findCommentsForTarget(targetType, targetId);
            Integer commentCount = commentRepo.countCommentsForTarget(targetType, targetId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("comments", comments);
            response.put("commentCount", commentCount);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}