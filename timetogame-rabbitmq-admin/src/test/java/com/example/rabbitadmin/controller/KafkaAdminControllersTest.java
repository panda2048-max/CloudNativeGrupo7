package com.example.rabbitadmin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.example.rabbitadmin.dto.kafka.ActualizarTopicoRequest;
import com.example.rabbitadmin.dto.kafka.CrearTopicoRequest;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.GrupoDetalle;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.MiembroInfo;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.OffsetInfo;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.ParticionInfo;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.TopicoDetalle;
import com.example.rabbitadmin.exception.ConflictoTopologiaException;
import com.example.rabbitadmin.security.RestAccessDeniedHandler;
import com.example.rabbitadmin.security.RestAuthenticationEntryPoint;
import com.example.rabbitadmin.security.SecurityConfig;
import com.example.rabbitadmin.service.KafkaAdminService;

@WebMvcTest(controllers = {KafkaTopicAdminController.class, KafkaConsumerGroupAdminController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.invalid/v2.0",
        "app.oidc.audience=test-audience"
})
class KafkaAdminControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KafkaAdminService kafkaAdminService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void exigeRolAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/kafka/topics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/kafka/topics").with(conRol("USER"))).andExpect(status().isForbidden());
    }

    @Test
    void creaUnTopicoValidoCon201YLocation() throws Exception {
        when(kafkaAdminService.crearTopico(any())).thenReturn(new TopicoDetalle("pedidos", false,
                List.of(new ParticionInfo(0, 1, List.of(1, 2, 3), List.of(1, 2, 3))), Map.of("cleanup.policy", "delete")));

        mockMvc.perform(post("/api/admin/kafka/topics").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"pedidos","particiones":3,"replicas":3,"politicaLimpieza":"DELETE",
                                 "retencionMs":604800000,"minInsyncReplicas":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/admin/kafka/topics/pedidos"))
                .andExpect(jsonPath("$.particiones[0].isr.length()").value(3));
    }

    @Test
    void rechazaNombresReservadosYValoresFueraDeRango() throws Exception {
        mockMvc.perform(post("/api/admin/kafka/topics").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"__interno","particiones":0,"replicas":5,"retencionMs":1000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.particiones").exists())
                .andExpect(jsonPath("$.errores.replicas").exists())
                .andExpect(jsonPath("$.errores.retencionValida").exists());
        verify(kafkaAdminService, never()).crearTopico(any(CrearTopicoRequest.class));
    }

    @Test
    void rechazaMinInsyncMayorQueLasReplicas() throws Exception {
        mockMvc.perform(post("/api/admin/kafka/topics").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"pedidos","particiones":3,"replicas":2,"minInsyncReplicas":3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.minInsyncCoherente").exists());
    }

    @Test
    void actualizarExigeAlMenosUnCambio() throws Exception {
        mockMvc.perform(patch("/api/admin/kafka/topics/pedidos").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.algunCambio").exists());
        verify(kafkaAdminService, never()).actualizarTopico(anyString(), any(ActualizarTopicoRequest.class));
    }

    @Test
    void actualizaLaRetencion() throws Exception {
        when(kafkaAdminService.actualizarTopico(eq("pedidos"), any())).thenReturn(new TopicoDetalle("pedidos", false,
                List.of(), Map.of("retention.ms", "86400000")));

        mockMvc.perform(patch("/api/admin/kafka/topics/pedidos").with(conRol("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"retencionMs\":86400000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuracion['retention.ms']").value("86400000"));
    }

    @Test
    void eliminarUnTopicoProtegidoResponde409() throws Exception {
        doThrow(new ConflictoTopologiaException("'timetogame.actividad' es un topico protegido"))
                .when(kafkaAdminService).eliminarTopico("timetogame.actividad");

        mockMvc.perform(delete("/api/admin/kafka/topics/timetogame.actividad").with(conRol("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("'timetogame.actividad' es un topico protegido"));
    }

    @Test
    void describeUnConsumerGroupConLag() throws Exception {
        when(kafkaAdminService.obtenerGrupo("tendencias")).thenReturn(new GrupoDetalle("tendencias", "STABLE", 1,
                "range", List.of(new MiembroInfo("c-1", "consumer-tendencias-1", "/10.0.0.5",
                        List.of("timetogame.actividad-0"))),
                List.of(new OffsetInfo("timetogame.actividad", 0, 90, 100, 10)), 10));

        mockMvc.perform(get("/api/admin/kafka/consumer-groups/tendencias").with(conRol("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lagTotal").value(10))
                .andExpect(jsonPath("$.miembros[0].particiones[0]").value("timetogame.actividad-0"));
    }

    private static RequestPostProcessor conRol(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
