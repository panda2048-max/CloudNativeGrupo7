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

/**
 * Documento generado de forma asincrona por FichaVideojuegoConsumer. Guarda el
 * id del videojuego sin clave foranea: la ficha se elimina por mensaje
 * (ficha.eliminar) despues de borrar el videojuego, no en la misma transaccion.
 */
@Entity
@Table(name = "fichas_videojuego")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FichaVideojuego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "videojuego_id", nullable = false, unique = true)
    private Long videojuegoId;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    /** Se incrementa en cada regeneracion. */
    @Column(nullable = false)
    private int version;

    /** Ultima solicitud aplicada: una entrega duplicada de la misma solicitud no regenera. */
    @Column(name = "ultima_solicitud_id", nullable = false, length = 36)
    private String ultimaSolicitudId;

    @Column(name = "solicitado_por", length = 120)
    private String solicitadoPor;

    @Column(name = "generado_en", nullable = false)
    private Instant generadoEn;
}
