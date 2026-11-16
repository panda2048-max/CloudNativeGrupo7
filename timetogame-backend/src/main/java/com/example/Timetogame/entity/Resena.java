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

/**
 * Resena de un usuario sobre un videojuego. Guarda el id del videojuego sin
 * clave foranea: al eliminar el juego, VideojuegoServiceImpl borra sus resenas
 * en la misma transaccion.
 */
@Entity
@Table(name = "resenas", indexes = {
        @Index(name = "idx_resenas_videojuego", columnList = "videojuego_id, creada_en"),
        @Index(name = "idx_resenas_creada", columnList = "creada_en")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "videojuego_id", nullable = false)
    private Long videojuegoId;

    @Column(name = "titulo_videojuego", nullable = false, length = 120)
    private String tituloVideojuego;

    @Column(nullable = false, length = 120)
    private String usuario;

    /** Estrellas de 1 a 5. */
    @Column(nullable = false)
    private int calificacion;

    @Column(nullable = false, length = 500)
    private String comentario;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;
}
