package com.example.Timetogame.dto;

import java.time.Instant;

import com.example.Timetogame.event.EntidadCatalogo;
import com.example.Timetogame.event.TipoEventoCatalogo;

public record NotificacionDTO(
        Long id,
        TipoEventoCatalogo tipoEvento,
        EntidadCatalogo entidad,
        Long entidadId,
        String mensaje,
        String usuario,
        Instant ocurridoEn,
        Instant procesadoEn
) {
}
