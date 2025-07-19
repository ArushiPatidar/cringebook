package com.cringebook.app.controllers;

import com.cringebook.app.entity.Comment;
import com.cringebook.app.entity.Like;
import com.cringebook.app.entity.Friendship;
import com.cringebook.app.repository.CommentRepo;
import com.cringebook.app.repository.LikeRepo;
import com.cringebook.app.repository.FriendshipRepo;
import com.cringebook.app.repository.UserRepo;
import com.cringebook.app.repository.MemoryRepo;
import com.cringebook.app.repository.EpisodeRepo;
import com.cringebook.app.repository.PhotoRepo;
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

    @Autowired
    private MemoryRepo memoryRepo;

    @Autowired
    private EpisodeRepo episodeRepo;

    @Autowired
    private PhotoRepo photoRepo;

    Authentication authentication = new Authentication();

    // Helper method to check if two users are friends
    private boolean areFriends(Integer userId1, Integer userId2) {
        if (userId1.equals(userId2)) {
            return false; // Same user, not a friendship
        }
        Friendship friendship = friendshipRepo.findFriendshipBetweenUsers(userId1, userId2);
        return friendship != null;
    }

    // Helper method to get content owner ID based on target type and ID
    private Integer getContentOwnerId(String targetType, Integer targetId) {
        try {
            switch (targetType.toLowerCase()) {
                case "memory":
                    var memory = memoryRepo.findById(targetId).orElse(null);
                    return memory != null ? memory.getUserId() : null;
                case "episode":
                    // Use the existing method to get user ID from episode
                    return episodeRepo.getUserIdFromEpisodeId(targetId);
                case "photo":
                    var photo = photoRepo.findById(targetId).orElse(null);
                    if (photo != null) {
                        // Get user ID from the episode that contains this photo
                        return photoRepo.getUserIdForEpisodeId(photo.getEpisodeId());
                    }
                    return null;
                case "comment":
                    var comment = commentRepo.findById(targetId).orElse(null);
                    if (comment != null) {
                        // For comment likes, get the content owner of the original content
                        return getContentOwnerId(comment.getTargetType(), comment.getTargetId());
                    }
                    return null;
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
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
            // Check friendship requirement for commenting
            // Need to get the content owner to check if they're friends
            Integer contentOwnerId = getContentOwnerId(targetType, targetId);
            if (contentOwnerId == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Content not found");
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            }
            
            // If user is trying to comment on their own content, allow it
            if (!userId.equals(contentOwnerId)) {
                // Check if users are friends
                if (!areFriends(userId, contentOwnerId)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "You can only comment on friends' content");
                    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
                }
            }
            
            Comment newComment = new Comment(userId, targetType, targetId, commentText.trim());
            Comment savedComment = commentRepo.save(newComment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("comment", savedComment);
            response.put("message", "Comment added");
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to add comment");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
            
            // Enhance comments with user information
            List<Map<String, Object>> enrichedComments = new java.util.ArrayList<>();
            for (Comment comment : comments) {
                Map<String, Object> enrichedComment = new HashMap<>();
                enrichedComment.put("commentId", comment.getCommentId());
                enrichedComment.put("userId", comment.getUserId());
                enrichedComment.put("commentText", comment.getCommentText());
                enrichedComment.put("createdAt", comment.getCreatedAt());
                
                // Add user information
                var user = userRepo.findByUserId(comment.getUserId());
                if (user != null) {
                    enrichedComment.put("userName", user.getUserName());
                    enrichedComment.put("userFullName", user.getName());
                } else {
                    enrichedComment.put("userName", "unknown");
                    enrichedComment.put("userFullName", "Unknown User");
                }
                
                enrichedComments.add(enrichedComment);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("comments", enrichedComments);
            response.put("commentCount", commentCount);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/nested-comments")
    public ResponseEntity<Map<String, Object>> getNestedComments(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken,
                                                                @RequestParam("targetType") String targetType,
                                                                @RequestParam("targetId") Integer targetId) {
        Integer userId = authentication.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        try {
            Map<String, Object> response = new HashMap<>();
            
            if ("memory".equals(targetType)) {
                // Get direct memory comments
                List<Comment> directComments = commentRepo.findCommentsForTarget("memory", targetId);
                response.put("directComments", enrichCommentsWithUserInfo(directComments));
                
                // Get episode comments within this memory
                List<Map<String, Object>> episodeComments = new java.util.ArrayList<>();
                List<com.cringebook.app.entity.Episode> episodes = episodeRepo.findByMemoryId(targetId);
                for (com.cringebook.app.entity.Episode episode : episodes) {
                    List<Comment> epComments = commentRepo.findCommentsForTarget("episode", episode.getEpisodeId());
                    for (Comment comment : epComments) {
                        Map<String, Object> enriched = createEnrichedComment(comment);
                        enriched.put("episodeTitle", episode.getTitle());
                        enriched.put("episodePhoto", episode.getPhoto());
                        episodeComments.add(enriched);
                    }
                    
                    // Get photo comments within this episode
                    List<com.cringebook.app.entity.Photo> photos = photoRepo.findByEpisodeId(episode.getEpisodeId());
                    for (com.cringebook.app.entity.Photo photo : photos) {
                        List<Comment> photoComments = commentRepo.findCommentsForTarget("photo", photo.getPhoto_id());
                        for (Comment comment : photoComments) {
                            Map<String, Object> enriched = createEnrichedComment(comment);
                            enriched.put("episodeTitle", episode.getTitle());
                            enriched.put("photoUrl", photo.getPhotoUrl());
                            enriched.put("photoCaption", ""); // Photo entity doesn't have caption
                            episodeComments.add(enriched);
                        }
                    }
                }
                response.put("nestedComments", episodeComments);
                
            } else if ("episode".equals(targetType)) {
                // Get direct episode comments
                List<Comment> directComments = commentRepo.findCommentsForTarget("episode", targetId);
                response.put("directComments", enrichCommentsWithUserInfo(directComments));
                
                // Get photo comments within this episode
                List<Map<String, Object>> photoComments = new java.util.ArrayList<>();
                List<com.cringebook.app.entity.Photo> photos = photoRepo.findByEpisodeId(targetId);
                for (com.cringebook.app.entity.Photo photo : photos) {
                    List<Comment> pComments = commentRepo.findCommentsForTarget("photo", photo.getPhoto_id());
                    for (Comment comment : pComments) {
                        Map<String, Object> enriched = createEnrichedComment(comment);
                        enriched.put("photoUrl", photo.getPhotoUrl());
                        enriched.put("photoCaption", ""); // Photo entity doesn't have caption
                        photoComments.add(enriched);
                    }
                }
                response.put("nestedComments", photoComments);
                
            } else {
                // For photos, just return direct comments
                List<Comment> directComments = commentRepo.findCommentsForTarget(targetType, targetId);
                response.put("directComments", enrichCommentsWithUserInfo(directComments));
                response.put("nestedComments", new java.util.ArrayList<>());
            }
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<Map<String, Object>> enrichCommentsWithUserInfo(List<Comment> comments) {
        List<Map<String, Object>> enrichedComments = new java.util.ArrayList<>();
        for (Comment comment : comments) {
            enrichedComments.add(createEnrichedComment(comment));
        }
        return enrichedComments;
    }

    private Map<String, Object> createEnrichedComment(Comment comment) {
        Map<String, Object> enrichedComment = new HashMap<>();
        enrichedComment.put("commentId", comment.getCommentId());
        enrichedComment.put("userId", comment.getUserId());
        enrichedComment.put("commentText", comment.getCommentText());
        enrichedComment.put("createdAt", comment.getCreatedAt());
        
        // Add user information
        var user = userRepo.findByUserId(comment.getUserId());
        if (user != null) {
            enrichedComment.put("userName", user.getUserName());
            enrichedComment.put("userFullName", user.getName());
        } else {
            enrichedComment.put("userName", "unknown");
            enrichedComment.put("userFullName", "Unknown User");
        }
        
        return enrichedComment;
    }
}