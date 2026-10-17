package com.example.Timetogame.messaging.config;

import java.util.Locale;

import com.example.Timetogame.event.CatalogoEvent;

/**
 * Nombres de la topologia RabbitMQ del caso de uso "notificaciones de catalogo".
 *
 * <pre>
 * Ruta principal:
 *   CatalogoEventProducer --(catalogo.&lt;entidad&gt;.&lt;tipo&gt;)--> [topic] timetogame.catalogo.events
 *       --(catalogo.#)--> [queue] timetogame.catalogo.notificaciones --> NotificacionCatalogoConsumer
 *
 * Reintento diferido (error recuperable, hasta N intentos):
 *   consumer --(notificaciones.retry)--> [direct] timetogame.catalogo.retry
 *       --> [queue] timetogame.catalogo.notificaciones.retry  (TTL; al expirar vuelve,
 *           via exchange por defecto, directo a la cola principal)
 *
 * Dead letter (error no recuperable o reintentos agotados -> basicNack sin requeue):
 *   [queue] timetogame.catalogo.notificaciones --(DLX)--> [direct] timetogame.catalogo.dlx
 *       --(notificaciones.dlq)--> [queue] timetogame.catalogo.notificaciones.dlq --> NotificacionDlqConsumer
 * </pre>
 */
public final class NotificacionesTopologia {

    public static final String EVENTS_EXCHANGE = "timetogame.catalogo.events";
    public static final String NOTIFICACIONES_QUEUE = "timetogame.catalogo.notificaciones";
    public static final String NOTIFICACIONES_BINDING_KEY = "catalogo.#";

    public static final String RETRY_EXCHANGE = "timetogame.catalogo.retry";
    public static final String RETRY_QUEUE = "timetogame.catalogo.notificaciones.retry";
    public static final String RETRY_ROUTING_KEY = "notificaciones.retry";

    public static final String DLX_EXCHANGE = "timetogame.catalogo.dlx";
    public static final String DLQ_QUEUE = "timetogame.catalogo.notificaciones.dlq";
    public static final String DLQ_ROUTING_KEY = "notificaciones.dlq";

    /** Header propio con la cantidad de reintentos ya realizados para un mensaje. */
    public static final String HEADER_REINTENTOS = "x-reintentos";
    /** Header propio con la causa del ultimo fallo, para diagnosticar desde la DLQ. */
    public static final String HEADER_ULTIMO_ERROR = "x-ultimo-error";
    /** Al pasar por la cola de reintento la routing key cambia; se conserva la original aqui. */
    public static final String HEADER_ROUTING_KEY_ORIGINAL = "x-routing-key-original";

    private NotificacionesTopologia() {
    }

    /** Ej: VIDEOJUEGO + CREADO -> "catalogo.videojuego.creado". */
    public static String routingKeyPara(CatalogoEvent evento) {
        return "catalogo." + evento.entidad().name().toLowerCase(Locale.ROOT)
                + "." + evento.tipo().name().toLowerCase(Locale.ROOT);
    }
}
