package com.cringebook.app.repository;

import com.cringebook.app.entity.Message;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MessageRepo extends CrudRepository<Message, Integer> {
    
    @Query(value = "SELECT * FROM messages WHERE " +
           "(sender_id = ?1 AND receiver_id = ?2) OR (sender_id = ?2 AND receiver_id = ?1) " +
           "ORDER BY sent_at ASC", nativeQuery = true)
    List<Message> findConversationBetweenUsers(Integer userId1, Integer userId2);
    
    @Query(value = "SELECT DISTINCT " +
           "CASE WHEN sender_id = ?1 THEN receiver_id ELSE sender_id END as friend_id " +
           "FROM messages WHERE sender_id = ?1 OR receiver_id = ?1", nativeQuery = true)
    List<Integer> findConversationPartners(Integer userId);
    
    @Query(value = "SELECT * FROM messages WHERE receiver_id = ?1 AND is_read = false " +
           "ORDER BY sent_at DESC", nativeQuery = true)
    List<Message> findUnreadMessages(Integer userId);
    
    @Query(value = "SELECT COUNT(*) FROM messages WHERE receiver_id = ?1 AND is_read = false", nativeQuery = true)
    Integer countUnreadMessages(Integer userId);
}