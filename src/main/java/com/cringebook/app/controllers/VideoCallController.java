package com.cringebook.app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VideoCallController implements WebSocketHandler {

    @Autowired
    private MessageWebSocketController messageWebSocketController;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userCallMap = new ConcurrentHashMap<>(); // userId -> sessionId
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            sessions.put(session.getId(), session);
            userCallMap.put(userId, session.getId());
            System.out.println("Video call session established for user: " + userId);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = (String) message.getPayload();
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        
        String type = (String) data.get("type");
        String fromUserId = getUserIdFromSession(session);
        
        switch (type) {
            case "call-request":
                handleCallRequest(fromUserId, data);
                break;
            case "call-response":
                handleCallResponse(fromUserId, data);
                break;
            case "offer":
            case "answer":
            case "ice-candidate":
                relaySignalingMessage(fromUserId, data);
                break;
            case "end-call":
                handleEndCall(fromUserId, data);
                break;
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = getUserIdFromSession(session);
        System.err.println("Transport error for user " + userId + ": " + exception.getMessage());
        cleanupSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String userId = getUserIdFromSession(session);
        System.out.println("Video call session closed for user: " + userId);
        cleanupSession(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String getUserIdFromSession(WebSocketSession session) {
        // Extract user ID from session attributes (set during connection)
        Object userIdObj = session.getAttributes().get("userId");
        return userIdObj != null ? userIdObj.toString() : null;
    }

    private void handleCallRequest(String fromUserId, Map<String, Object> data) throws IOException {
        String toUserId = (String) data.get("toUserId");
        String sessionId = userCallMap.get(toUserId);
        
        if (sessionId != null) {
            WebSocketSession targetSession = sessions.get(sessionId);
            if (targetSession != null && targetSession.isOpen()) {
                Map<String, Object> callRequest = Map.of(
                    "type", "incoming-call",
                    "fromUserId", fromUserId,
                    "fromUserName", data.get("fromUserName")
                );
                sendMessage(targetSession, callRequest);
                
                // Send notification popup for video call
                if (messageWebSocketController != null) {
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("fromUserId", fromUserId);
                    notification.put("fromUserName", data.get("fromUserName"));
                    
                    messageWebSocketController.sendNotification(Integer.parseInt(toUserId), "video_call", notification);
                }
            }
        }
    }

    private void handleCallResponse(String fromUserId, Map<String, Object> data) throws IOException {
        String toUserId = (String) data.get("toUserId");
        boolean accepted = (Boolean) data.get("accepted");
        String sessionId = userCallMap.get(toUserId);
        
        if (sessionId != null) {
            WebSocketSession targetSession = sessions.get(sessionId);
            if (targetSession != null && targetSession.isOpen()) {
                Map<String, Object> response = Map.of(
                    "type", "call-response",
                    "fromUserId", fromUserId,
                    "accepted", accepted
                );
                sendMessage(targetSession, response);
            }
        }
    }

    private void relaySignalingMessage(String fromUserId, Map<String, Object> data) throws IOException {
        String toUserId = (String) data.get("toUserId");
        String sessionId = userCallMap.get(toUserId);
        
        if (sessionId != null) {
            WebSocketSession targetSession = sessions.get(sessionId);
            if (targetSession != null && targetSession.isOpen()) {
                data.put("fromUserId", fromUserId);
                sendMessage(targetSession, data);
            }
        }
    }

    private void handleEndCall(String fromUserId, Map<String, Object> data) throws IOException {
        String toUserId = (String) data.get("toUserId");
        String sessionId = userCallMap.get(toUserId);
        
        if (sessionId != null) {
            WebSocketSession targetSession = sessions.get(sessionId);
            if (targetSession != null && targetSession.isOpen()) {
                Map<String, Object> endCall = Map.of(
                    "type", "call-ended",
                    "fromUserId", fromUserId
                );
                sendMessage(targetSession, endCall);
            }
        }
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }

    private void cleanupSession(WebSocketSession session) {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            userCallMap.remove(userId);
        }
        sessions.remove(session.getId());
    }
}