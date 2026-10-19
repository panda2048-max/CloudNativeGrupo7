package com.example.Timetogame.messaging.config;

import java.time.Duration;

/** A donde se publica la copia a reintentar y cuantas veces como maximo. */
public record PoliticaReintento(
        String retryExchange,
        String retryRoutingKey,
        int maxReintentos,
        Duration retraso
) {
}
