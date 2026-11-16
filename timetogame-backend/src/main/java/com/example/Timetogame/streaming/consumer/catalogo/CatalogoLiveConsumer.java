package com.example.Timetogame.streaming.consumer.catalogo;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.Timetogame.service.AvisosCatalogoService;
import com.example.Timetogame.streaming.config.TopologiaKafka;
import com.example.Timetogame.streaming.consumer.EventoKafkaValidator;
import com.example.Timetogame.streaming.event.CatalogoCambioEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer group "catalogo-live": avisa en vivo cada cambio del catalogo a
 * los navegadores conectados por SSE. Lee desde el final del topico
 * (auto-offset-reset=latest): al arrancar no tiene sentido re-avisar cambios viejos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogoLiveConsumer {

    private final EventoKafkaValidator validator;
    private final AvisosCatalogoService avisosCatalogoService;

    @KafkaListener(id = "catalogo-live", topics = TopologiaKafka.CATALOGO_CAMBIOS,
            groupId = TopologiaKafka.GRUPO_CATALOGO_LIVE,
            properties = {
                    "spring.json.value.default.type=com.example.Timetogame.streaming.event.CatalogoCambioEvent",
                    "auto.offset.reset=latest"})
    public void onCambioCatalogo(ConsumerRecord<String, CatalogoCambioEvent> record) {
        CatalogoCambioEvent evento = validator.validar(record);
        avisosCatalogoService.notificarCambio(evento);
        log.info("Aviso en vivo: {} videojuego {} ({})", evento.tipo(), evento.videojuegoId(),
                EventoKafkaValidator.ubicacion(record));
    }
}
