package com.example.Timetogame.dto;

import java.util.List;

public record LoginResponseDTO(
        String token,
        String tokenType,
        String username,
        List<String> roles,
        long expiresInMs
) {
}
