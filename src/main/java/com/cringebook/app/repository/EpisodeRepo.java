package com.cringebook.app.repository;

import com.cringebook.app.entity.Episode;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;


public interface EpisodeRepo extends CrudRepository<Episode, Integer> {
    @Query(value = "SELECT * FROM episode WHERE BINARY memory_id = ?1", nativeQuery = true)
    public List<Episode> findByMemoryId(Integer memory_id);

    @Query(value = "select user_id from memories where memory_id = ( select memory_id from episode where episode_id = ?1)", nativeQuery = true)
    public Integer getUserIdFromEpisodeId(Integer episodeId);
}
