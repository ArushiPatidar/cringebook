package com.cringebook.app.entity;


import jakarta.persistence.*;

@Entity(name="episode")
public class Episode {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name="episode_id")
    private Integer episodeId;

    @Column(name="memory_id")
    private Integer memoryId;

    @Column(name="photo_url")
    private String photo;

    @Column(name="description")
    private String description;

    @Column(name="title")
    private String title;

    public Episode(){}

    public Episode(Integer memoryId, String title, String photo, String description){
        this.memoryId = memoryId;
        this.title = title;
        this.photo = photo;
        this.description = description;
    }

    public Integer getEpisodeId() {
        return episodeId;
    }

    public Integer getMemoryId() {
        return memoryId;
    }

    public String getPhoto() {
        return photo;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
