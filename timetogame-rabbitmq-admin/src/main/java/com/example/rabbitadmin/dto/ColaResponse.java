package com.example.rabbitadmin.dto;

import java.util.Map;

/** mensajes/consumidores pueden ser null si el broker aun no tiene estadisticas de la cola. */
public record ColaResponse(
        String nombre,
        String tipo,
        boolean durable,
        boolean autoDelete,
        Map<String, Object> argumentos,
        Long mensajes,
        Integer consumidores
) {
}
