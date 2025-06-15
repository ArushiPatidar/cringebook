package com.cringebook.app.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cringebook.app.entity.User;
import com.cringebook.app.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@RestController
public class Authentication {

    Algorithm algorithm = Algorithm.HMAC256("Govind");
    JWTVerifier verifier = JWT.require(algorithm)
            .withIssuer("cringeBook")
            .build();

    @Autowired
    private UserRepo userRepo;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(String username, String password, String name, String phoneNo, String email){

        User user = new User(username, password, name, email, phoneNo);

        try{
            User InsertedUser = userRepo.save(user);
            String jwtToken = getToken(InsertedUser.getUserID());
            return new ResponseEntity<>(jwtToken, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("something went wrong", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(String username, String password){
        User user = userRepo.findByUsername(username);
        if ( user.getPassword().equals(password) ){
            String jwtToken = getToken(user.getUserID());
            return new ResponseEntity<>(jwtToken, HttpStatus.OK);
        }else{
            return new ResponseEntity<>("Not found", HttpStatus.UNAUTHORIZED);
        }

    }

    @GetMapping("/home")
    public String home(@RequestHeader(HttpHeaders.AUTHORIZATION) String jwtToken){
        Integer userId = getIdFromToken(jwtToken);
        return "Hello world to mr " + userId;
    }

    public Integer getIdFromToken(String jwtToken){
        try {
            DecodedJWT decodedJWT = verifier.verify(jwtToken);
            Claim claim = decodedJWT.getClaim("userId");
            return claim.asInt();
        } catch (JWTVerificationException e) {
            System.out.println(e.getMessage());
            return 0;
        }
    }

    private String getToken(Integer userId){
        return JWT.create()
                .withIssuer("cringeBook")
                .withSubject("user Details")
                .withClaim("userId", userId)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 500000000L))
                .withJWTId(UUID.randomUUID()
                        .toString())
                .withNotBefore(new Date(System.currentTimeMillis() + 1000L))
                .sign(algorithm);

    }

}
