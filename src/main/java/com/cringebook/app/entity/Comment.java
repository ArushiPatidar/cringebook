package com.cringebook.app.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Integer commentId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "target_type") // "memory", "episode", "photo"
    private String targetType;

    @Column(name = "target_id")
    private Integer targetId;

    @Column(name = "comment_text", length = 1000)
    private String commentText;

    @Column(name = "created_at")
    private java.sql.Timestamp createdAt;

    public Comment() {
        this.createdAt = new java.sql.Timestamp(System.currentTimeMillis());
    }

    public Comment(Integer userId, String targetType, Integer targetId, String commentText) {
        this.userId = userId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.commentText = commentText;
        this.createdAt = new java.sql.Timestamp(System.currentTimeMillis());
    }

    // Getters and setters
    public Integer getCommentId() { return commentId; }
    public void setCommentId(Integer commentId) { this.commentId = commentId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }
}