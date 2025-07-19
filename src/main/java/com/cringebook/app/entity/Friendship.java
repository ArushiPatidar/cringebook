package com.cringebook.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity(name="friendships")
public class Friendship {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name="friendship_id")
    private Integer friendshipId;

    @Column(name="user1_id")
    private Integer user1Id;

    @Column(name="user2_id")
    private Integer user2Id;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    public Friendship() {
        this.createdAt = LocalDateTime.now();
    }

    public Friendship(Integer user1Id, Integer user2Id) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getFriendshipId() {
        return friendshipId;
    }

    public Integer getUser1Id() {
        return user1Id;
    }

    public Integer getUser2Id() {
        return user2Id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}