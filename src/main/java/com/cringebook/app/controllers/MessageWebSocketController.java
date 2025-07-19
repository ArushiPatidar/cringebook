package com.cringebook.app.controllers;

import com.cringebook.app.entity.Message;
import com.cringebook.app.entity.User;
import com.cringebook.app.repository.MessageRepo;
import com.cringebook.app.repository.UserRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageWebSocketController extends TextWebSocketHandler {

    @Autowired
    private MessageRepo messageRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private Authentication authService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.put(userId, session);
            System.out.println("Message WebSocket connected for user: " + userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
            System.out.println("Message WebSocket disconnected for user: " + userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) return;

        try {
            // Parse incoming message
            Map<String, Object> messageData = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) messageData.get("type");

            if ("send_message".equals(type)) {
                handleSendMessage(Integer.parseInt(userId), messageData);
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }
    }

    private void handleSendMessage(int senderId, Map<String, Object> messageData) {
        try {
            int receiverId = (Integer) messageData.get("receiverId");
            String messageText = (String) messageData.get("messageText");

            // Save message to database using constructor
            Message message = new Message(senderId, receiverId, messageText);
            Message savedMessage = messageRepo.save(message);

            // Get sender info
            Optional<User> senderOpt = userRepo.findById(senderId);
            if (!senderOpt.isPresent()) return;

            User sender = senderOpt.get();

            // Prepare message data for real-time delivery
            Map<String, Object> realTimeMessage = new HashMap<>();
            realTimeMessage.put("type", "new_message");
            realTimeMessage.put("messageId", savedMessage.getMessageId());
            realTimeMessage.put("senderId", senderId);
            realTimeMessage.put("senderName", sender.getName());
            realTimeMessage.put("receiverId", receiverId);
            realTimeMessage.put("messageText", messageText);
            realTimeMessage.put("timestamp", savedMessage.getSentAt().toString());
            realTimeMessage.put("isRead", false);

            // Send to receiver if online
            WebSocketSession receiverSession = userSessions.get(String.valueOf(receiverId));
            if (receiverSession != null && receiverSession.isOpen()) {
                try {
                    receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(realTimeMessage)));
                    
                    // Also send a notification popup for the message
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("senderId", senderId);
                    notification.put("senderName", sender.getName());
                    notification.put("messageText", messageText);
                    notification.put("timestamp", savedMessage.getSentAt().toString());
                    
                    sendNotification(receiverId, "new_message", notification);
                } catch (IOException e) {
                    System.err.println("Failed to send message to receiver: " + e.getMessage());
                }
            }

            // Send confirmation back to sender
            WebSocketSession senderSession = userSessions.get(String.valueOf(senderId));
            if (senderSession != null && senderSession.isOpen()) {
                Map<String, Object> confirmation = new HashMap<>();
                confirmation.put("type", "message_sent");
                confirmation.put("messageId", savedMessage.getMessageId());
                confirmation.put("receiverId", receiverId);
                confirmation.put("timestamp", savedMessage.getSentAt().toString());
                
                try {
                    senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmation)));
                } catch (IOException e) {
                    System.err.println("Failed to send confirmation to sender: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error handling send message: " + e.getMessage());
        }
    }

    // Method to notify users about message read status
    public void notifyMessageRead(int senderId, int receiverId, int messageId) {
        WebSocketSession senderSession = userSessions.get(String.valueOf(senderId));
        if (senderSession != null && senderSession.isOpen()) {
            try {
                Map<String, Object> readNotification = new HashMap<>();
                readNotification.put("type", "message_read");
                readNotification.put("messageId", messageId);
                readNotification.put("readBy", receiverId);
                
                senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(readNotification)));
            } catch (IOException e) {
                System.err.println("Failed to send read notification: " + e.getMessage());
            }
        }
    }

    // Method to send notifications to users (can be called from other controllers)
    public void sendNotification(int userId, String notificationType, Map<String, Object> notificationData) {
        WebSocketSession session = userSessions.get(String.valueOf(userId));
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "notification");
                notification.put("notificationType", notificationType);
                notification.putAll(notificationData);
                
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(notification)));
            } catch (IOException e) {
                System.err.println("Failed to send notification to user " + userId + ": " + e.getMessage());
            }
        }
    }

    // Method to get connected users (for debugging/monitoring)
    public Set<String> getConnectedUsers() {
        return userSessions.keySet();
    }
}