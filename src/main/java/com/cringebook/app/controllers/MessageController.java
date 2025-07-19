package com.cringebook.app.controllers;

import com.cringebook.app.entity.Message;
import com.cringebook.app.entity.User;
import com.cringebook.app.entity.Friendship;
import com.cringebook.app.repository.MessageRepo;
import com.cringebook.app.repository.UserRepo;
import com.cringebook.app.repository.FriendshipRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageRepo messageRepo;
    
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private FriendshipRepo friendshipRepo;
    
    @Autowired
    private Authentication authService;

    // Send a message
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken,
            @RequestParam Integer receiverId,
            @RequestParam String messageText) {
        
        Integer senderId = authService.getIdFromToken(jwtToken);
        if (senderId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            // Check if users are friends
            Friendship friendship = friendshipRepo.findFriendshipBetweenUsers(senderId, receiverId);
            if (friendship == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "You can only message friends");
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
            }
            
            // Create and save message
            Message message = new Message(senderId, receiverId, messageText);
            Message savedMessage = messageRepo.save(message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", savedMessage);
            response.put("success", true);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send message");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get conversation between two users
    @GetMapping("/conversation/{friendId}")
    public ResponseEntity<Map<String, Object>> getConversation(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken,
            @PathVariable Integer friendId) {
        
        Integer userId = authService.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            // Check if users are friends
            Friendship friendship = friendshipRepo.findFriendshipBetweenUsers(userId, friendId);
            if (friendship == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "You can only message friends");
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
            }
            
            // Get conversation
            List<Message> messages = messageRepo.findConversationBetweenUsers(userId, friendId);
            
            // Mark messages as read if the current user is the receiver
            for (Message message : messages) {
                if (message.getReceiverId().equals(userId) && !message.getIsRead()) {
                    message.setIsRead(true);
                    messageRepo.save(message);
                }
            }
            
            // Get friend info
            User friend = userRepo.findByUserId(friendId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("messages", messages);
            response.put("friend", friend);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load conversation");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get list of all conversations
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken) {
        
        Integer userId = authService.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            List<Integer> partnerIds = messageRepo.findConversationPartners(userId);
            List<Map<String, Object>> conversations = new ArrayList<>();
            
            for (Integer partnerId : partnerIds) {
                // Check if still friends
                Friendship friendship = friendshipRepo.findFriendshipBetweenUsers(userId, partnerId);
                if (friendship != null) {
                    User partner = userRepo.findByUserId(partnerId);
                    if (partner != null) {
                        // Get last message in conversation
                        List<Message> conversationMessages = messageRepo.findConversationBetweenUsers(userId, partnerId);
                        Message lastMessage = conversationMessages.isEmpty() ? null : 
                            conversationMessages.get(conversationMessages.size() - 1);
                        
                        // Count unread messages from this partner
                        int unreadCount = 0;
                        for (Message msg : conversationMessages) {
                            if (msg.getReceiverId().equals(userId) && !msg.getIsRead()) {
                                unreadCount++;
                            }
                        }
                        
                        Map<String, Object> conversation = new HashMap<>();
                        conversation.put("partner", partner);
                        conversation.put("lastMessage", lastMessage);
                        conversation.put("unreadCount", unreadCount);
                        
                        conversations.add(conversation);
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("conversations", conversations);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load conversations");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get unread message count
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken) {
        
        Integer userId = authService.getIdFromToken(jwtToken);
        if (userId == 0) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            Integer unreadCount = messageRepo.countUnreadMessages(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("unreadCount", unreadCount);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get unread count");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}