package com.example.Timetogame.streaming.sse;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.Timetogame.service.TendenciasService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Conexiones Server-Sent Events abiertas por los navegadores. Empuja dos
 * tipos de evento:
 * <ul>
 *   <li>{@code tendencias}: el ranking completo, como maximo una vez por
 *       segundo y solo si cambio (una rafaga de 5.000 vistas no genera 5.000
 *       mensajes al navegador).</li>
 *   <li>{@code catalogo}: un aviso por cada cambio del catalogo.</li>
 * </ul>
 * Un comentario cada 15 s mantiene viva la conexion a traves de proxies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamHub {

    private static final Duration DURACION_CONEXION = Duration.ofMinutes(30);

    private final TendenciasService tendenciasService;
    private final List<SseEmitter> emisores = new CopyOnWriteArrayList<>();

    public SseEmitter suscribir() {
        SseEmitter emisor = new SseEmitter(DURACION_CONEXION.toMillis());
        emisor.onCompletion(() -> emisores.remove(emisor));
        emisor.onTimeout(() -> emisores.remove(emisor));
        emisor.onError(error -> emisores.remove(emisor));
        emisores.add(emisor);
        // El navegador recibe el estado actual al conectarse, sin esperar el proximo cambio.
        enviarA(emisor, SseEmitter.event().name("tendencias").data(tendenciasService.ranking()));
        return emisor;
    }

    public void enviar(String evento, Object datos) {
        for (SseEmitter emisor : emisores) {
            enviarA(emisor, SseEmitter.event().name(evento).data(datos));
        }
    }

    @Scheduled(fixedDelay = 1_000)
    void emitirTendenciasSiCambiaron() {
        if (tendenciasService.consumirCambios() && !emisores.isEmpty()) {
            enviar("tendencias", tendenciasService.ranking());
        }
    }

    @Scheduled(fixedRate = 15_000)
    void latido() {
        for (SseEmitter emisor : emisores) {
            enviarA(emisor, SseEmitter.event().comment("ping"));
        }
    }

    private void enviarA(SseEmitter emisor, SseEmitter.SseEventBuilder evento) {
        try {
            emisor.send(evento);
        } catch (IOException | IllegalStateException e) {
            // El navegador cerro la pestana: se descarta la conexion.
            emisores.remove(emisor);
            log.debug("Conexion SSE cerrada: {}", e.getMessage());
        }
    }
}
