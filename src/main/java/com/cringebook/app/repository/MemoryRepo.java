package com.cringebook.app.repository;

import com.cringebook.app.entity.Episode;
import com.cringebook.app.entity.Memory;
import com.cringebook.app.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MemoryRepo extends CrudRepository<Memory, Integer> {
    @Query(value = "SELECT * FROM memories WHERE BINARY user_id = ?1", nativeQuery = true)
    public List<Memory> findByUserId(Integer user_id);

    @Query(value="UPDATE memory SET photo_url = ?4, title = ?2, description = ?3 WHERE memory_id = ?1", nativeQuery = true)
    public Memory updateMemory(Integer memoryId, String title, String description, String photo);
}
