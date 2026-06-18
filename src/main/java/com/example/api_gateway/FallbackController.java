package com.example.api_gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {

    @Value("${gateway.fallback.error-message}")
    private String errorMessage;

    @Value("${gateway.fallback.error-key}")
    private String errorKey;

    @Value("${gateway.fallback.correlation-id-key}")
    private String correlationIdKey;

    @GetMapping("/fallback")
    public ResponseEntity<Map<String, Object>> fallback(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(GlobalCorrelationIdFilter.CORRELATION_ID_HEADER);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        errorKey, errorMessage,
                        correlationIdKey, correlationId
                ));
    }
}
