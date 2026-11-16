package com.example.Timetogame.streaming.event;

import java.time.Instant;

import com.example.Timetogame.event.TipoEventoCatalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Un videojuego se creo, actualizo o elimino (topico timetogame.catalogo.cambios,
 * compactado por key = videojuegoId: queda el ultimo estado de cada juego).
 *
 * @param eventId      el mismo id del evento de catalogo que viaja por RabbitMQ: se puede seguir un
 *                     cambio en ambos brokers
 * @param ocurridoEn   cuando se confirmo el cambio en la BD
 * @param tipo         CREADO, ACTUALIZADO o ELIMINADO: los consumidores deciden que hacer con el juego
 * @param videojuegoId key del mensaje
 * @param titulo       titulo vigente (o el ultimo que tuvo, si se elimino)
 * @param usuario      quien hizo el cambio
 */
public record CatalogoCambioEvent(
        @NotBlank @Size(max = 36) String eventId,
        @NotNull Instant ocurridoEn,
        @NotNull TipoEventoCatalogo tipo,
        @NotNull @Positive Long videojuegoId,
        @NotBlank @Size(max = 120) String titulo,
        @Size(max = 120) String usuario
) {
}
