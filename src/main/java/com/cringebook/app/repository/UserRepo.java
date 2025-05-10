package com.cringebook.app.repository;

import com.cringebook.app.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface UserRepo extends CrudRepository<User, Integer> {
    @Query(value = "SELECT * FROM user_table WHERE username = ?1", nativeQuery = true)
    public User findByUsername(String username);
}
