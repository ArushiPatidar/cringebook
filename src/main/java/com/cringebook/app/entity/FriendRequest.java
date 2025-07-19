package com.cringebook.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity(name="friend_requests")
public class FriendRequest {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name="request_id")
    private Integer requestId;

    @Column(name="requester_id")
    private Integer requesterId;

    @Column(name="recipient_id") 
    private Integer recipientId;

    @Column(name="status")
    private String status; // PENDING, ACCEPTED, REJECTED

    @Column(name="created_at")
    private LocalDateTime createdAt;

    public FriendRequest() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    public FriendRequest(Integer requesterId, Integer recipientId) {
        this.requesterId = requesterId;
        this.recipientId = recipientId;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public Integer getRequestId() {
        return requestId;
    }

    public Integer getRequesterId() {
        return requesterId;
    }

    public Integer getRecipientId() {
        return recipientId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}