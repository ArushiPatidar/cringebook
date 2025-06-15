package com.cringebook.app.repository;

import com.cringebook.app.entity.Episode;
import com.cringebook.app.entity.Photo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;


public interface PhotoRepo extends CrudRepository<Photo, Integer> {

    @Query(value = "select user_id from memories where memory_id = ( select memory_id from episode where episode_id = ?1) LIMIT 0, 1000", nativeQuery = true)
    public Integer getUserIdForEpisodeId(Integer episodeId);

    @Query(value = "SELECT * FROM photo WHERE BINARY episode_id = ?1", nativeQuery = true)
    public List<Photo> findByEpisodeId(Integer episodeId);
}
