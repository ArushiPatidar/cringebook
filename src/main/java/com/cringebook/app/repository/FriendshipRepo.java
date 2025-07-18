package com.cringebook.app.repository;

import com.cringebook.app.entity.Friendship;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FriendshipRepo extends CrudRepository<Friendship, Integer> {
    
    @Query(value = "SELECT * FROM friendships WHERE (user1_id = ?1 AND user2_id = ?2) OR (user1_id = ?2 AND user2_id = ?1)", nativeQuery = true)
    Friendship findFriendshipBetweenUsers(Integer userId1, Integer userId2);
    
    @Query(value = "SELECT * FROM friendships WHERE user1_id = ?1 OR user2_id = ?1", nativeQuery = true)
    List<Friendship> findFriendshipsByUser(Integer userId);
}