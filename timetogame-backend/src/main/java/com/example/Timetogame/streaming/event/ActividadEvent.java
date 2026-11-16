package com.example.Timetogame.streaming.event;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Algo que un usuario hizo con un videojuego (topico timetogame.actividad,
 * key = videojuegoId). Un solo topico para toda la actividad: cada consumer
 * group toma los tipos que le interesan.
 *
 * @param eventId      identificador unico: permite descartar duplicados (Kafka entrega "al menos una vez")
 * @param ocurridoEn   cuando ocurrio (tiempo del evento, no de procesamiento): la ventana de tendencias
 *                     se calcula con este valor
 * @param tipo         VISTA (abrio la ficha) o RESENA (publico una resena): decide que consumidor actua
 * @param videojuegoId sobre que juego; tambien es la key, asi toda la actividad de un juego cae en la
 *                     misma particion y se procesa en orden
 * @param titulo       viaja con el evento para que los consumidores no consulten la BD
 * @param usuario      quien lo hizo (null = visitante sin sesion; una resena siempre tiene usuario)
 * @param origen       distingue trafico real de la simulacion de carga
 * @param calificacion solo RESENA: estrellas de 1 a 5
 * @param comentario   solo RESENA: el texto de la resena
 */
public record ActividadEvent(
        @NotBlank @Size(max = 36) String eventId,
        @NotNull Instant ocurridoEn,
        @NotNull TipoActividad tipo,
        @NotNull @Positive Long videojuegoId,
        @NotBlank @Size(max = 120) String titulo,
        @Size(max = 120) String usuario,
        @NotNull OrigenActividad origen,
        @Min(1) @Max(5) Integer calificacion,
        @Size(max = 500) String comentario
) {

    public static ActividadEvent vista(Long videojuegoId, String titulo, String usuario, OrigenActividad origen) {
        return new ActividadEvent(UUID.randomUUID().toString(), Instant.now(), TipoActividad.VISTA, videojuegoId,
                titulo, usuario, origen, null, null);
    }

    public static ActividadEvent resena(Long videojuegoId, String titulo, String usuario, int calificacion,
            String comentario, Instant ocurridoEn) {
        return new ActividadEvent(UUID.randomUUID().toString(), ocurridoEn, TipoActividad.RESENA, videojuegoId,
                titulo, usuario, OrigenActividad.DETALLE, calificacion, comentario);
    }

    /** Una resena trae calificacion, comentario y usuario; una vista no trae calificacion ni comentario. */
    @AssertTrue(message = "los datos no corresponden al tipo de actividad")
    public boolean isDatosCoherentesConElTipo() {
        if (tipo == null) {
            return true;
        }
        return switch (tipo) {
            case VISTA -> calificacion == null && comentario == null;
            case RESENA -> calificacion != null && comentario != null && !comentario.isBlank() && usuario != null;
        };
    }
}
