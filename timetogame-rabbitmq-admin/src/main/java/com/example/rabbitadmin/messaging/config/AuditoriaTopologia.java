package com.example.rabbitadmin.messaging.config;

import java.util.Locale;

import com.example.rabbitadmin.event.OperacionTopologiaEvent;

/**
 * Nombres de la topologia RabbitMQ del caso de uso "auditoria de la
 * administracion": cada cambio hecho por esta API queda registrado de forma
 * asincrona, sin demorar la respuesta HTTP.
 *
 * <pre>
 * Ruta principal:
 *   AuditoriaEventProducer --(admin.&lt;recurso&gt;.&lt;operacion&gt;)--> [topic] timetogame.admin.auditoria
 *       --(admin.#)--> [queue] timetogame.admin.auditoria.registro --> AuditoriaConsumer
 *
 * Dead letter (payload invalido o fallo al registrar -> basicNack sin requeue):
 *   [queue] timetogame.admin.auditoria.registro --(DLX)--> [direct] timetogame.admin.auditoria.dlx
 *       --(auditoria.dlq)--> [queue] timetogame.admin.auditoria.dlq --> AuditoriaDlqConsumer
 * </pre>
 *
 * Sin cola de reintento: registrar en el log no depende de recursos externos,
 * asi que un fallo aqui no es transitorio y reintentarlo daria lo mismo.
 */
public final class AuditoriaTopologia {

    public static final String AUDITORIA_EXCHANGE = "timetogame.admin.auditoria";
    public static final String AUDITORIA_QUEUE = "timetogame.admin.auditoria.registro";
    public static final String AUDITORIA_BINDING_KEY = "admin.#";

    public static final String AUDITORIA_DLX = "timetogame.admin.auditoria.dlx";
    public static final String AUDITORIA_DLQ = "timetogame.admin.auditoria.dlq";
    public static final String AUDITORIA_DLQ_ROUTING_KEY = "auditoria.dlq";

    private AuditoriaTopologia() {
    }

    /** Ej: COLA + CREAR -> "admin.cola.crear". */
    public static String routingKeyPara(OperacionTopologiaEvent evento) {
        return "admin." + evento.recurso().name().toLowerCase(Locale.ROOT)
                + "." + evento.operacion().name().toLowerCase(Locale.ROOT);
    }
}
