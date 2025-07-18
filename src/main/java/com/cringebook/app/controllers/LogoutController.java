package com.cringebook.app.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class LogoutController {

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        try {
            // Clear the Authorization cookie
            Cookie cookie = new Cookie("Authorization", "");
            cookie.setMaxAge(0); // Expire immediately
            cookie.setPath("/"); // Same path as when it was set
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
            
            return new ResponseEntity<>("Logged out successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error during logout", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}