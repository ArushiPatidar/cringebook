package com.cringebook.app.repository;

import com.cringebook.app.entity.Interest;
import org.springframework.data.repository.CrudRepository;

public interface InterestRepo extends CrudRepository<Interest, Integer> {
}
