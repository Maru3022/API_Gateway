package com.example.api_gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatusCode status = ex instanceof ResponseStatusException responseStatus
                ? responseStatus.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR;
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        String requestId = exchange.getResponse().getHeaders()
                .getFirst(GlobalCorrelationIdFilter.CORRELATION_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            exchange.getResponse().getHeaders().set(GlobalCorrelationIdFilter.CORRELATION_ID_HEADER, requestId);
        }
        // Do not log exception messages: connection errors may contain credentials or internal URLs.
        log.warn("requestId={}, status={}, exceptionType={}", requestId, status.value(), ex.getClass().getSimpleName());
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", exchange.getRequest().getPath().value());
        errorResponse.put("status", status.value());
        errorResponse.put("error", knownStatus == null ? "Request failed" : knownStatus.getReasonPhrase());
        errorResponse.put("requestId", requestId);

        exchange.getResponse().setStatusCode(status);
        if (ex instanceof ResponseStatusException responseStatus) {
            exchange.getResponse().getHeaders().addAll(responseStatus.getHeaders());
        }
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = bufferFactory.wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
