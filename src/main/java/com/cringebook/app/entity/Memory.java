package com.cringebook.app.entity;

import jakarta.persistence.*;

@Entity(name="memories")
public class Memory {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name="memory_id")
    private Integer memoryId;

    @Column(name="user_id")
    private Integer userId;

    @Column(name="photo_url")
    private String photo;

    @Column(name="title")
    private String title;

    @Column(name="description")
    private String description;

    public Memory(){}

    public Memory(Integer userId, String photo, String title, String description){
        this.userId = userId;
        this.photo = photo;
        this.title = title;
        this.description = description;
    }

    public Integer getMemoryId() {
        return memoryId;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getPhoto() {
        return photo;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
