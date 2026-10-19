package com.example.Timetogame.messaging.config;

import java.util.Locale;

import com.example.Timetogame.event.AccionFicha;
import com.example.Timetogame.event.CatalogoEvent;

/**
 * Unico lugar con los nombres de exchanges, colas, routing keys y headers de
 * RabbitMQ del backend. Las clases *MessagingConfig declaran los beans a partir
 * de estas constantes y los productores/consumidores solo las referencian.
 *
 * <pre>
 * NOTIFICACIONES (evento de catalogo -> notificacion para administradores)
 *   CatalogoEventProducer --(catalogo.&lt;entidad&gt;.&lt;tipo&gt;)--> [topic] timetogame.catalogo.events
 *       --(catalogo.#)--> [quorum] timetogame.catalogo.notificaciones --> NotificacionCatalogoConsumer
 *   reintento: --(notificaciones.retry)--> [direct] timetogame.catalogo.retry
 *       --> [quorum, TTL] timetogame.catalogo.notificaciones.retry --(al expirar)--> cola principal
 *   DLQ: --(DLX)--> [direct] timetogame.catalogo.dlx --(notificaciones.dlq)-->
 *       [quorum] timetogame.catalogo.notificaciones.dlq --> NotificacionDlqConsumer
 *
 * DOCUMENTOS (generacion asincrona de la ficha tecnica de un videojuego)
 *   SolicitudFichaProducer --(ficha.generar | ficha.eliminar)--> [direct] timetogame.documentos
 *       --> [quorum] timetogame.documentos.fichas --> FichaVideojuegoConsumer
 *   reintento: --(fichas.retry)--> [direct] timetogame.documentos.retry
 *       --> [quorum, TTL] timetogame.documentos.fichas.retry --(al expirar)--> cola principal
 *   DLQ: --(DLX)--> [direct] timetogame.documentos.dlx --(fichas.dlq)-->
 *       [quorum] timetogame.documentos.fichas.dlq --> FichaDlqConsumer
 * </pre>
 */
public final class TopologiaMensajeria {

    private TopologiaMensajeria() {
    }

    /** Eventos de dominio del catalogo (fuente de las notificaciones). */
    public static final class Catalogo {

        public static final String EVENTS_EXCHANGE = "timetogame.catalogo.events";

        private Catalogo() {
        }

        /** Ej: VIDEOJUEGO + CREADO -> "catalogo.videojuego.creado". */
        public static String routingKeyPara(CatalogoEvent evento) {
            return "catalogo." + evento.entidad().name().toLowerCase(Locale.ROOT)
                    + "." + evento.tipo().name().toLowerCase(Locale.ROOT);
        }
    }

    public static final class Notificaciones {

        public static final String QUEUE = "timetogame.catalogo.notificaciones";
        public static final String BINDING_KEY = "catalogo.#";

        public static final String RETRY_EXCHANGE = "timetogame.catalogo.retry";
        public static final String RETRY_QUEUE = "timetogame.catalogo.notificaciones.retry";
        public static final String RETRY_ROUTING_KEY = "notificaciones.retry";

        public static final String DLX_EXCHANGE = "timetogame.catalogo.dlx";
        public static final String DLQ_QUEUE = "timetogame.catalogo.notificaciones.dlq";
        public static final String DLQ_ROUTING_KEY = "notificaciones.dlq";

        private Notificaciones() {
        }
    }

    public static final class Documentos {

        public static final String EXCHANGE = "timetogame.documentos";
        public static final String FICHAS_QUEUE = "timetogame.documentos.fichas";
        public static final String GENERAR_ROUTING_KEY = "ficha.generar";
        public static final String ELIMINAR_ROUTING_KEY = "ficha.eliminar";

        public static final String RETRY_EXCHANGE = "timetogame.documentos.retry";
        public static final String RETRY_QUEUE = "timetogame.documentos.fichas.retry";
        public static final String RETRY_ROUTING_KEY = "fichas.retry";

        public static final String DLX_EXCHANGE = "timetogame.documentos.dlx";
        public static final String DLQ_QUEUE = "timetogame.documentos.fichas.dlq";
        public static final String DLQ_ROUTING_KEY = "fichas.dlq";

        private Documentos() {
        }

        public static String routingKeyPara(AccionFicha accion) {
            return switch (accion) {
                case GENERAR -> GENERAR_ROUTING_KEY;
                case ELIMINAR -> ELIMINAR_ROUTING_KEY;
            };
        }
    }

    /** Headers propios que viajan con los mensajes reintentados. */
    public static final class Headers {

        /** Cantidad de reintentos ya realizados para un mensaje. */
        public static final String REINTENTOS = "x-reintentos";
        /** Causa del ultimo fallo, para diagnosticar desde la DLQ. */
        public static final String ULTIMO_ERROR = "x-ultimo-error";
        /** Al pasar por la cola de reintento la routing key cambia; se conserva la original aqui. */
        public static final String ROUTING_KEY_ORIGINAL = "x-routing-key-original";

        private Headers() {
        }
    }
}
