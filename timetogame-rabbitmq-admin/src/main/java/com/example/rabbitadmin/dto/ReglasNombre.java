package com.example.rabbitadmin.dto;

/**
 * Reglas de validacion compartidas para nombres de colas/exchanges y routing
 * keys. RabbitMQ acepta mas caracteres, pero restringirlos evita nombres
 * ambiguos en URLs, logs y en la UI de management.
 */
public final class ReglasNombre {

    /** Empieza con letra o numero, luego letras/numeros/. _ : -, max 255, y nunca "amq." (reservado por RabbitMQ). */
    public static final String PATRON_NOMBRE = "^(?!amq\\.)[A-Za-z0-9][A-Za-z0-9._:-]{0,254}$";
    public static final String MENSAJE_NOMBRE = "solo letras, numeros y los caracteres . _ : -, debe empezar con "
            + "letra o numero, maximo 255 caracteres y no puede empezar con 'amq.' (prefijo reservado por RabbitMQ)";

    /** Para referenciar recursos que ya existen (incluye los "amq.*" del broker, que se pueden consultar). */
    public static final String PATRON_NOMBRE_EXISTENTE = "^[A-Za-z0-9][A-Za-z0-9._:-]{0,254}$";
    public static final String MENSAJE_NOMBRE_EXISTENTE = "nombre invalido: solo letras, numeros y . _ : -";

    /** Routing key: puede estar vacia (fanout) y admite los comodines de topic (* y #). */
    public static final String PATRON_ROUTING_KEY = "^[A-Za-z0-9._:*#-]{0,255}$";
    public static final String MENSAJE_ROUTING_KEY = "solo letras, numeros, los caracteres . _ : - y los comodines "
            + "* y #, maximo 255 caracteres";

    private ReglasNombre() {
    }
}
