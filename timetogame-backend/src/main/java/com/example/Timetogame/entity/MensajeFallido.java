package com.example.Timetogame.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mensajes_fallidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeFallido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", length = 64)
    private String messageId;

    @Column(name = "cola_origen", length = 255)
    private String colaOrigen;

    @Column(name = "routing_key", length = 255)
    private String routingKey;

    /** Motivo reportado por el broker en x-death: rejected, expired, maxlen... */
    @Column(length = 40)
    private String motivo;

    @Column(nullable = false)
    private int reintentos;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "registrado_en", nullable = false)
    private Instant registradoEn;
}
