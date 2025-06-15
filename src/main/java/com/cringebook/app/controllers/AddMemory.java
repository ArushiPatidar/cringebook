package com.cringebook.app.controllers;

import com.cringebook.app.entity.Episode;
import com.cringebook.app.entity.Memory;
import com.cringebook.app.repository.MemoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
public class AddMemory {

    @Autowired
    private MemoryRepo memoryRepo;

    Authentication authentication = new Authentication();

//    public Authentication getAuthentication() {
//        authentication.getIdFromToken();
//        return authentication;
//    }

    @GetMapping("/show_memory")
    public ResponseEntity<List<Memory>> getMemory(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, Integer userId) {
        Integer userId2 = authentication.getIdFromToken(jwtToken);
        if (userId !=0 & Objects.equals(userId2, userId)){
            try{
                List<Memory> memories= memoryRepo.findByUserId(userId);
                return new ResponseEntity<>(memories, HttpStatus.OK);
            }catch (Exception e){
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @PostMapping("/save memory")
    public ResponseEntity<Integer> saveMemory(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, String photo, String title, String description){
        Integer userId = authentication.getIdFromToken(jwtToken);
        if (userId != 0) {
            Memory memory = new Memory(userId, photo, title, description);
            try {
                Memory InsertedMemory = memoryRepo.save(memory);
                return new ResponseEntity<>(InsertedMemory.getMemoryId(), HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>(0, HttpStatus.FORBIDDEN);
            }
        }
        return new ResponseEntity<>(0, HttpStatus.NOT_FOUND);
    }

    @PutMapping("/update memory")
    public ResponseEntity<Integer> updateMemory(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, Integer memoryId, String photo, String title, String description){
        Integer userId = authentication.getIdFromToken(jwtToken);
        Optional<Memory> memory = memoryRepo.findById(memoryId);
        if (memory.isPresent()){
            Memory memory1 = memory.get();
            if (description != null) memory1.setDescription(description);
            if (title != null) memory1.setTitle(title);
            if (photo != null) memory1.setPhoto(photo);
            try{
                memoryRepo.save(memory1);
                return new ResponseEntity<>(1, HttpStatus.OK);
            }catch (Exception e) {
                return new ResponseEntity<>(0, HttpStatus.BAD_REQUEST);
            }
        }else{
            return new ResponseEntity<>(0, HttpStatus.NO_CONTENT);
        }
    }

    @DeleteMapping("/delete memory")
    public ResponseEntity<Integer> deleteMemory(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken, Integer memoryId){
        Optional<Memory> memory = memoryRepo.findById(memoryId);
        if (memory.isPresent()){
            try{
                memoryRepo.deleteById(memoryId);
                return new ResponseEntity<>(1, HttpStatus.OK);
            }catch (Exception e){
                return new ResponseEntity<>(0, HttpStatus.BAD_REQUEST);
            }
        }else{
            return new ResponseEntity<>(0, HttpStatus.NO_CONTENT);
        }
    }
}
