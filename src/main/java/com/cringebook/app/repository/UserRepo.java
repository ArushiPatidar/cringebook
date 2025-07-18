package com.cringebook.app.repository;

import com.cringebook.app.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepo extends CrudRepository<User, Integer> {
    @Query(value = "SELECT * FROM user_table WHERE username = ?1", nativeQuery = true)
    public User findByUsername(String username);
    
    @Query(value = "SELECT * FROM user_table WHERE username LIKE %?1% OR name LIKE %?1% OR email LIKE %?1%", nativeQuery = true)
    public List<User> searchUsers(String searchTerm);
    
    @Query(value = "SELECT * FROM user_table WHERE user_id = ?1", nativeQuery = true)
    public User findByUserId(Integer userId);
}
