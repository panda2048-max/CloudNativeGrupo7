package com.example.rabbitadmin.dto.kafka;

import java.util.Locale;

/** cleanup.policy de un topico. */
public enum PoliticaLimpieza {
    /** Borra los mensajes por tiempo o tamano (flujos de eventos). */
    DELETE,
    /** Conserva el ultimo mensaje de cada key (estado actual de cada entidad). */
    COMPACT;

    public String valorKafka() {
        return name().toLowerCase(Locale.ROOT);
    }
}
