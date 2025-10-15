package com.example.publickeyinfrastructure.dto;

public record LoginRequest(
        String email,
        String password
) {}
