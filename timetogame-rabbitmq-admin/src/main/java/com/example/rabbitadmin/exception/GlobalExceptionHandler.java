package com.example.rabbitadmin.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RecursoNoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoTopologiaException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictoTopologiaException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ConfiguracionInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleConfiguracionInvalida(ConfiguracionInvalidaException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BrokerNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleBrokerNoDisponible(BrokerNoDisponibleException ex) {
        log.error("Broker no disponible (RabbitMQ o Kafka)", ex);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    /** Errores de validacion del cuerpo (DTO con @Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                errores.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return buildValidationResponse(errores);
    }

    /** Errores de validacion de @PathVariable / @RequestParam. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleParameterValidation(HandlerMethodValidationException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            // Un @Valid @RequestBody en un metodo que ademas valida @PathVariable llega
            // aqui (no como MethodArgumentNotValidException): se informa por campo del DTO.
            if (result instanceof ParameterErrors erroresDto) {
                erroresDto.getFieldErrors().forEach(error ->
                        errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
                erroresDto.getGlobalErrors().forEach(error ->
                        errores.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
                return;
            }
            String parametro = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error ->
                    errores.putIfAbsent(parametro != null ? parametro : "parametro", error.getDefaultMessage()));
        });
        return buildValidationResponse(errores);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return buildValidationResponse(Map.of(ex.getParameterName(), "El parametro es obligatorio"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Cuerpo de la peticion invalido: JSON mal formado o valor no permitido (revise tipo/tipoCola)");
    }

    private ResponseEntity<Map<String, Object>> buildValidationResponse(Map<String, String> errores) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("mensaje", "La peticion tiene parametros invalidos");
        body.put("errores", errores);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("mensaje", message);
        return ResponseEntity.status(status).body(body);
    }
}
