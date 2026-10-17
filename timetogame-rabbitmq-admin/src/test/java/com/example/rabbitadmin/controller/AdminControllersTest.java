package com.example.rabbitadmin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.example.rabbitadmin.dto.BindingResponse;
import com.example.rabbitadmin.dto.ColaResponse;
import com.example.rabbitadmin.exception.ConflictoTopologiaException;
import com.example.rabbitadmin.exception.RecursoNoEncontradoException;
import com.example.rabbitadmin.security.RestAccessDeniedHandler;
import com.example.rabbitadmin.security.RestAuthenticationEntryPoint;
import com.example.rabbitadmin.security.SecurityConfig;
import com.example.rabbitadmin.service.RabbitAdminService;

@WebMvcTest(controllers = {QueueAdminController.class, ExchangeAdminController.class, BindingAdminController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.invalid/v2.0",
        "app.oidc.audience=test-audience"
})
class AdminControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RabbitAdminService rabbitAdminService;

    /** Reemplaza el decoder real, que consultaria el issuer de Azure al arrancar. */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ------------------------------------------------------------- seguridad

    @Test
    void rechazaPeticionesSinToken() throws Exception {
        mockMvc.perform(get("/api/admin/queues"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Debes autenticarte para acceder a este recurso"));
    }

    @Test
    void rechazaUsuariosSinRolAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/queues").with(conRol("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteListarConRolAdmin() throws Exception {
        when(rabbitAdminService.listarColas()).thenReturn(List.of(
                new ColaResponse("pedidos", "classic", true, false, Map.of(), 3L, 1)));

        mockMvc.perform(get("/api/admin/queues").with(conRol("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("pedidos"))
                .andExpect(jsonPath("$[0].mensajes").value(3));
    }

    // ----------------------------------------------------------------- colas

    @Test
    void crearColaValidaDevuelve201ConLocation() throws Exception {
        when(rabbitAdminService.crearCola(any())).thenReturn(
                new ColaResponse("pedidos", "classic", true, false, Map.of(), 0L, 0));

        mockMvc.perform(post("/api/admin/queues").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"pedidos\",\"ttlMensajesMs\":60000}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/admin/queues/pedidos"))
                .andExpect(jsonPath("$.nombre").value("pedidos"));
    }

    @Test
    void noPermiteCrearColasConNombreVacio() throws Exception {
        mockMvc.perform(post("/api/admin/queues").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
        verify(rabbitAdminService, never()).crearCola(any());
    }

    @Test
    void noPermiteNombresReservadosPorRabbitMQ() throws Exception {
        mockMvc.perform(post("/api/admin/queues").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"amq.mi-cola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    void noPermiteNombresConCaracteresInvalidos() throws Exception {
        mockMvc.perform(post("/api/admin/queues").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"cola con espacios\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    void rechazaColaQuorumAutoDelete() throws Exception {
        mockMvc.perform(post("/api/admin/queues").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"q1\",\"tipo\":\"QUORUM\",\"autoDelete\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.configuracionQuorumValida").exists());
    }

    @Test
    void rechazaDeadLetterRoutingKeySinExchange() throws Exception {
        mockMvc.perform(post("/api/admin/queues").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"q1\",\"deadLetterRoutingKey\":\"dlq\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.deadLetterConsistente").exists());
    }

    @Test
    void rechazaTtlYMaximoFueraDeRango() throws Exception {
        mockMvc.perform(post("/api/admin/queues").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"q1\",\"ttlMensajesMs\":-5,\"maxMensajes\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.ttlMensajesMs").exists())
                .andExpect(jsonPath("$.errores.maxMensajes").exists());
    }

    @Test
    void eliminarColaDevuelve204YPropagaLasCondiciones() throws Exception {
        mockMvc.perform(delete("/api/admin/queues/pedidos").with(conRol("ADMIN"))
                        .param("soloSiVacia", "true"))
                .andExpect(status().isNoContent());
        verify(rabbitAdminService).eliminarCola("pedidos", false, true);
    }

    @Test
    void eliminarColaInexistenteDevuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe la cola 'x'"))
                .when(rabbitAdminService).eliminarCola(anyString(), anyBoolean(), anyBoolean());

        mockMvc.perform(delete("/api/admin/queues/x").with(conRol("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe la cola 'x'"));
    }

    @Test
    void eliminarColaProtegidaDevuelve409() throws Exception {
        doThrow(new ConflictoTopologiaException("recurso protegido"))
                .when(rabbitAdminService).eliminarCola(anyString(), anyBoolean(), anyBoolean());

        mockMvc.perform(delete("/api/admin/queues/timetogame.catalogo.notificaciones").with(conRol("ADMIN")))
                .andExpect(status().isConflict());
    }

    @Test
    void validaElNombreEnElPath() throws Exception {
        mockMvc.perform(delete("/api/admin/queues/{nombre}", "cola invalida!").with(conRol("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
        verify(rabbitAdminService, never()).eliminarCola(anyString(), anyBoolean(), anyBoolean());
    }

    // ------------------------------------------------------------- exchanges

    @Test
    void crearExchangeSinTipoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/admin/exchanges").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"pedidos.events\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipo").exists());
    }

    @Test
    void crearExchangeConTipoDesconocidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/admin/exchanges").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"pedidos.events\",\"tipo\":\"RARO\"}"))
                .andExpect(status().isBadRequest());
        verify(rabbitAdminService, never()).crearExchange(any());
    }

    // -------------------------------------------------------------- bindings

    @Test
    void crearBindingDevuelve201() throws Exception {
        when(rabbitAdminService.crearBinding(any())).thenReturn(
                new BindingResponse("pedidos.events", "pedidos", "pedido.#"));

        mockMvc.perform(post("/api/admin/bindings").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exchange\":\"pedidos.events\",\"cola\":\"pedidos\",\"routingKey\":\"pedido.#\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routingKey").value("pedido.#"));
    }

    @Test
    void crearBindingSinColaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/admin/bindings").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exchange\":\"pedidos.events\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cola").exists());
    }

    @Test
    void eliminarBindingValidaLaRoutingKey() throws Exception {
        mockMvc.perform(delete("/api/admin/bindings/pedidos.events/pedidos").with(conRol("ADMIN"))
                        .param("routingKey", "con espacio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.routingKey").exists());
    }

    @Test
    void eliminarBindingDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/admin/bindings/pedidos.events/pedidos").with(conRol("ADMIN"))
                        .param("routingKey", "pedido.#"))
                .andExpect(status().isNoContent());
        verify(rabbitAdminService).eliminarBinding("pedidos.events", "pedidos", "pedido.#");
    }

    private static RequestPostProcessor conRol(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
