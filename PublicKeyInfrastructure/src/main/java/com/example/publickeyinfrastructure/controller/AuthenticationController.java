package com.example.publickeyinfrastructure.controller;

import com.example.publickeyinfrastructure.dto.LoginRequest;
import com.example.publickeyinfrastructure.dto.LoginResponse;
import com.example.publickeyinfrastructure.dto.RegisterRequest;
import com.example.publickeyinfrastructure.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final UserService userService;

    public AuthenticationController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok("Proverite vaš email za link za verifikaciju.");
    }

    @GetMapping("/confirm")
    public ResponseEntity<String> confirm(@RequestParam("token") String token) {
        String result = userService.confirmToken(token);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }
}
