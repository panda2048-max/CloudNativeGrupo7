package com.example.rabbitadmin.dto;

public record ExchangeResponse(
        String nombre,
        String tipo,
        boolean durable,
        boolean autoDelete
) {
}
