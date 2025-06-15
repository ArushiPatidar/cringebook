package com.cringebook.app.entity;

import jakarta.persistence.*;

@Entity(name="photo")
public class Photo {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name="photo_id")
    private Integer photo_id;

    @Column(name="episode_id")
    private Integer episodeId;

    @Column(name="photo_url")
    private String photoUrl;

    public Photo(){}

    public Photo( Integer episodeId, String photoUrl){

        this.episodeId = episodeId;
        this.photoUrl = photoUrl;
    }

    public Integer getPhoto_id() {
        return photo_id;
    }

    public Integer getEpisodeId() {
        return episodeId;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
