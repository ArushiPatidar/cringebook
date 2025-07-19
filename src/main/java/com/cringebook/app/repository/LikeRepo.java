package com.cringebook.app.repository;

import com.cringebook.app.entity.Like;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LikeRepo extends CrudRepository<Like, Integer> {
    
    @Query(value = "SELECT * FROM likes WHERE user_id = ?1 AND target_type = ?2 AND target_id = ?3", nativeQuery = true)
    Like findByUserAndTarget(Integer userId, String targetType, Integer targetId);
    
    @Query(value = "SELECT COUNT(*) FROM likes WHERE target_type = ?1 AND target_id = ?2", nativeQuery = true)
    Integer countLikesForTarget(String targetType, Integer targetId);
    
    @Query(value = "SELECT * FROM likes WHERE target_type = ?1 AND target_id = ?2", nativeQuery = true)
    List<Like> findLikesForTarget(String targetType, Integer targetId);
    
    @Query(value = "DELETE FROM likes WHERE user_id = ?1 AND target_type = ?2 AND target_id = ?3", nativeQuery = true)
    void deleteLike(Integer userId, String targetType, Integer targetId);
}