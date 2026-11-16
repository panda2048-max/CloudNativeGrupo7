package com.example.Timetogame.streaming.event;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Alguien abrio la ficha de un videojuego (topico timetogame.actividad,
 * key = videojuegoId).
 *
 * @param eventId     identificador unico: permite descartar duplicados (Kafka entrega "al menos una vez")
 * @param ocurridoEn  cuando ocurrio la vista (tiempo del evento, no de procesamiento): la ventana de
 *                    tendencias se calcula con este valor
 * @param videojuegoId que se vio; tambien es la key, asi todas las vistas de un juego caen en la misma
 *                    particion y se procesan en orden
 * @param titulo      viaja con el evento para que los consumidores no consulten la BD
 * @param usuario     quien lo vio (null = visitante sin sesion)
 * @param origen      distingue trafico real de la simulacion de carga
 */
public record VideojuegoVistoEvent(
        @NotBlank @Size(max = 36) String eventId,
        @NotNull Instant ocurridoEn,
        @NotNull @Positive Long videojuegoId,
        @NotBlank @Size(max = 120) String titulo,
        @Size(max = 120) String usuario,
        @NotNull OrigenVista origen
) {

    public static VideojuegoVistoEvent de(Long videojuegoId, String titulo, String usuario, OrigenVista origen) {
        return new VideojuegoVistoEvent(UUID.randomUUID().toString(), Instant.now(), videojuegoId, titulo, usuario,
                origen);
    }
}
