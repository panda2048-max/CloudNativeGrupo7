package com.example.rabbitadmin.dto.kafka;

/** Reglas de validacion compartidas por los DTO y rutas de topicos. */
public final class ReglasTopico {

    /**
     * Caracteres que Kafka admite en un nombre de topico, hasta 249. No puede
     * empezar con "__" (reservado para topicos internos como __consumer_offsets).
     */
    public static final String PATRON_NOMBRE = "^(?!__)[A-Za-z0-9._-]{1,249}$";
    public static final String MENSAJE_NOMBRE =
            "solo letras, numeros, '.', '_' y '-' (1 a 249) y sin el prefijo reservado '__'";

    /** Para consultar o eliminar se aceptan tambien los internos. */
    public static final String PATRON_NOMBRE_EXISTENTE = "^[A-Za-z0-9._-]{1,249}$";
    public static final String MENSAJE_NOMBRE_EXISTENTE = "Nombre de topico invalido";

    public static final String PATRON_GRUPO = "^[A-Za-z0-9._-]{1,255}$";
    public static final String MENSAJE_GRUPO = "Id de consumer group invalido";

    /** Minimo razonable de retencion: menos de 1 minuto borraria mensajes antes de consumirlos. */
    public static final long RETENCION_MINIMA_MS = 60_000L;
    /** -1 = retencion infinita. */
    public static final long RETENCION_INFINITA = -1L;

    private ReglasTopico() {
    }

    public static boolean retencionValida(Long retencionMs) {
        return retencionMs == null || retencionMs == RETENCION_INFINITA || retencionMs >= RETENCION_MINIMA_MS;
    }
}
