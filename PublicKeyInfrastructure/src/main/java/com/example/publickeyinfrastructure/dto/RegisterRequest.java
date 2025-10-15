package com.example.publickeyinfrastructure.dto;

public record RegisterRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        String organization
) {}
