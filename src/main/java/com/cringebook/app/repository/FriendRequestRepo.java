package com.cringebook.app.repository;

import com.cringebook.app.entity.FriendRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FriendRequestRepo extends CrudRepository<FriendRequest, Integer> {
    
    @Query(value = "SELECT * FROM friend_requests WHERE recipient_id = ?1 AND status = 'PENDING'", nativeQuery = true)
    List<FriendRequest> findPendingRequestsByRecipient(Integer recipientId);
    
    @Query(value = "SELECT * FROM friend_requests WHERE requester_id = ?1 AND recipient_id = ?2", nativeQuery = true)
    FriendRequest findByRequesterAndRecipient(Integer requesterId, Integer recipientId);
    
    @Query(value = "SELECT * FROM friend_requests WHERE (requester_id = ?1 AND recipient_id = ?2) OR (requester_id = ?2 AND recipient_id = ?1)", nativeQuery = true)
    FriendRequest findRequestBetweenUsers(Integer userId1, Integer userId2);
}