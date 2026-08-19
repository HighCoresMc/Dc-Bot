package com.integrafty.opexy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    // Main Route
    @GetMapping("/")
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("Opexy Bot is running");
    }
}
