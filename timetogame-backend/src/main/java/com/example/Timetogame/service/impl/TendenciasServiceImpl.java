package com.example.Timetogame.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Service;

import com.example.Timetogame.dto.TendenciaDTO;
import com.example.Timetogame.event.TipoEventoCatalogo;
import com.example.Timetogame.service.TendenciasService;
import com.example.Timetogame.streaming.config.StreamingProperties;
import com.example.Timetogame.streaming.event.CatalogoCambioEvent;
import com.example.Timetogame.streaming.event.VideojuegoVistoEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Conteo en memoria por minuto (tiempo del evento) y suma de los minutos que
 * caen dentro de la ventana. Es seguro con varios hilos consumidores: cada
 * particion la procesa un solo hilo, pero distintos juegos se actualizan en
 * paralelo.
 *
 * <p>Al reiniciar se pierde el conteo; como el ranking solo mira los ultimos
 * minutos, se repone solo con el trafico nuevo.
 */
@Slf4j
@Service
public class TendenciasServiceImpl implements TendenciasService {

    private static final int MAX_EVENTOS_RECORDADOS = 50_000;

    private final StreamingProperties.Tendencias config;
    private final Map<Long, Contador> contadores = new ConcurrentHashMap<>();
    private final Set<Long> eliminados = ConcurrentHashMap.newKeySet();
    private final Set<String> eventosContados = Collections.newSetFromMap(Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_EVENTOS_RECORDADOS;
                }
            }));
    private final AtomicBoolean cambios = new AtomicBoolean(true);

    public TendenciasServiceImpl(StreamingProperties properties) {
        this.config = properties.tendencias();
    }

    @Override
    public boolean registrarVista(VideojuegoVistoEvent evento) {
        simularCostoDeProcesamiento();

        long minuto = minutoDe(evento.ocurridoEn());
        if (minuto < minutoInicioVentana() || eliminados.contains(evento.videojuegoId())) {
            return false;
        }
        if (!eventosContados.add(evento.eventId())) {
            return false;
        }
        // compute es atomico por juego: la limpieza de consumirCambios no puede
        // borrar el contador mientras se le suma una vista.
        contadores.compute(evento.videojuegoId(), (id, contador) -> {
            Contador actual = contador != null ? contador : new Contador();
            actual.titulo = evento.titulo();
            actual.porMinuto.computeIfAbsent(minuto, m -> new LongAdder()).increment();
            return actual;
        });
        cambios.set(true);
        return true;
    }

    @Override
    public void aplicarCambio(CatalogoCambioEvent evento) {
        if (evento.tipo() == TipoEventoCatalogo.ELIMINADO) {
            eliminados.add(evento.videojuegoId());
            contadores.remove(evento.videojuegoId());
        } else {
            eliminados.remove(evento.videojuegoId());
            Contador contador = contadores.get(evento.videojuegoId());
            if (contador != null) {
                contador.titulo = evento.titulo();
            }
        }
        cambios.set(true);
    }

    @Override
    public List<TendenciaDTO> ranking() {
        long inicio = minutoInicioVentana();
        return contadores.entrySet().stream()
                .map(e -> new TendenciaDTO(e.getKey(), e.getValue().titulo, e.getValue().vistasDesde(inicio)))
                .filter(t -> t.vistas() > 0)
                .sorted(Comparator.comparingLong(TendenciaDTO::vistas).reversed()
                        .thenComparing(TendenciaDTO::videojuegoId))
                .limit(config.top())
                .toList();
    }

    @Override
    public boolean consumirCambios() {
        // La ventana avanza aunque no lleguen eventos: se limpia lo vencido y,
        // si algo salio, el ranking tambien cambio.
        long inicio = minutoInicioVentana();
        for (Long id : List.copyOf(contadores.keySet())) {
            contadores.computeIfPresent(id, (clave, contador) -> {
                if (contador.porMinuto.keySet().removeIf(m -> m < inicio)) {
                    cambios.set(true);
                }
                return contador.porMinuto.isEmpty() ? null : contador;
            });
        }
        return cambios.getAndSet(false);
    }

    private long minutoInicioVentana() {
        return minutoDe(Instant.now().minus(config.ventana())) + 1;
    }

    private static long minutoDe(Instant instante) {
        return instante.getEpochSecond() / 60;
    }

    private void simularCostoDeProcesamiento() {
        Duration costo = config.costoProcesamiento();
        if (costo.isZero()) {
            return;
        }
        try {
            Thread.sleep(costo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class Contador {
        private volatile String titulo;
        private final Map<Long, LongAdder> porMinuto = new ConcurrentHashMap<>();

        private long vistasDesde(long minutoInicio) {
            return porMinuto.entrySet().stream()
                    .filter(e -> e.getKey() >= minutoInicio)
                    .mapToLong(e -> e.getValue().sum())
                    .sum();
        }
    }
}
