package com.example.Timetogame.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Vista de un videojuego por un usuario con sesion; la escribe el consumer group "historial". */
@Entity
@Table(name = "historial_vistas", indexes = @Index(name = "idx_historial_usuario", columnList = "usuario, visto_en"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialVista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unico: una entrega repetida del mismo evento no duplica la fila. */
    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "videojuego_id", nullable = false)
    private Long videojuegoId;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 120)
    private String usuario;

    @Column(name = "visto_en", nullable = false)
    private Instant vistoEn;

    @Column(name = "registrado_en", nullable = false)
    private Instant registradoEn;
}
