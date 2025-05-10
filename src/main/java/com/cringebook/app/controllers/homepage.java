package com.cringebook.app.controllers;

import com.cringebook.app.entity.Interest;
import com.cringebook.app.repository.InterestRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class homepage {

    @Autowired
    private InterestRepo interestRepo;

//    @GetMapping("/home")
//    public String home(){
//
//        return "Hello world";
//    }

    @GetMapping("/show")
    public List<Interest> show() {
        List<Interest> interests = (List<Interest>) interestRepo.findAll();
        for (Interest interest : interestRepo.findAll()) {
            System.out.println(interest.getIntital_balance());
        }
        return interests;
    }

    @PostMapping("/sayHello")
    public String sayHello(String name) {
        return "Hello " + name ;
    }

}

