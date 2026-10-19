package com.example.Timetogame.messaging.producer;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.Timetogame.event.AccionFicha;
import com.example.Timetogame.event.CatalogoEvent;
import com.example.Timetogame.event.EntidadCatalogo;
import com.example.Timetogame.event.SolicitudFicha;
import com.example.Timetogame.event.TipoEventoCatalogo;
import com.example.Timetogame.messaging.config.TopologiaMensajeria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Encola la generacion de fichas tecnicas. Hay dos origenes, ambos eventos de
 * aplicacion (los servicios no conocen RabbitMQ):
 * <ul>
 *   <li>Un cambio en un videojuego ({@link CatalogoEvent}): se crea/actualiza
 *       -> GENERAR, se elimina -> ELIMINAR.</li>
 *   <li>Una solicitud explicita ({@link SolicitudFicha}) desde
 *       POST /api/videojuegos/{id}/ficha.</li>
 * </ul>
 * En los dos casos se publica despues del commit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitudFichaProducer {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCambioDeCatalogo(CatalogoEvent evento) {
        if (evento.entidad() != EntidadCatalogo.VIDEOJUEGO) {
            return;
        }
        AccionFicha accion = evento.tipo() == TipoEventoCatalogo.ELIMINADO ? AccionFicha.ELIMINAR : AccionFicha.GENERAR;
        publicar(SolicitudFicha.de(accion, evento.entidadId(), evento.usuario()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSolicitudExplicita(SolicitudFicha solicitud) {
        publicar(solicitud);
    }

    private void publicar(SolicitudFicha solicitud) {
        String routingKey = TopologiaMensajeria.Documentos.routingKeyPara(solicitud.accion());
        try {
            rabbitTemplate.convertAndSend(TopologiaMensajeria.Documentos.EXCHANGE, routingKey, solicitud,
                    message -> {
                        message.getMessageProperties().setMessageId(solicitud.solicitudId());
                        return message;
                    },
                    new CorrelationData(solicitud.solicitudId()));
            log.info("Solicitud de ficha publicada: solicitudId={}, routingKey={}, videojuegoId={}",
                    solicitud.solicitudId(), routingKey, solicitud.videojuegoId());
        } catch (AmqpException e) {
            log.error("No se pudo publicar la solicitud de ficha {} ({}): broker no disponible",
                    solicitud.solicitudId(), routingKey, e);
        }
    }
}
