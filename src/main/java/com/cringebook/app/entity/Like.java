package com.cringebook.app.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "likes")
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Integer likeId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "target_type") // "memory", "episode", "photo", "comment"
    private String targetType;

    @Column(name = "target_id")
    private Integer targetId;

    @Column(name = "created_at")
    private java.sql.Timestamp createdAt;

    public Like() {
        this.createdAt = new java.sql.Timestamp(System.currentTimeMillis());
    }

    public Like(Integer userId, String targetType, Integer targetId) {
        this.userId = userId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.createdAt = new java.sql.Timestamp(System.currentTimeMillis());
    }

    // Getters and setters
    public Integer getLikeId() { return likeId; }
    public void setLikeId(Integer likeId) { this.likeId = likeId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }

    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }
}