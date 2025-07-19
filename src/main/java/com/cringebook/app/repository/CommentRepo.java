package com.cringebook.app.repository;

import com.cringebook.app.entity.Comment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CommentRepo extends CrudRepository<Comment, Integer> {
    
    @Query(value = "SELECT * FROM comments WHERE target_type = ?1 AND target_id = ?2 ORDER BY created_at ASC", nativeQuery = true)
    List<Comment> findCommentsForTarget(String targetType, Integer targetId);
    
    @Query(value = "SELECT COUNT(*) FROM comments WHERE target_type = ?1 AND target_id = ?2", nativeQuery = true)
    Integer countCommentsForTarget(String targetType, Integer targetId);
}