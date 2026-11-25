package com.example.api_gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GatewayErrorHandlingTest {
    private final GlobalErrorWebExceptionHandler handler = new GlobalErrorWebExceptionHandler(new ObjectMapper());

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    void fallbackAcceptsOriginalMethodWithoutReplayingOperation(String method) {
        var controller = new FallbackController();
        ReflectionTestUtils.setField(controller, "errorMessage", "Service temporarily unavailable");
        ReflectionTestUtils.setField(controller, "errorKey", "error");
        ReflectionTestUtils.setField(controller, "correlationIdKey", "correlationId");
        var client = WebTestClient.bindToController(controller).build();
        client.method(HttpMethod.valueOf(method)).uri("/fallback")
                .header("X-Correlation-Id", "request-123")
                .exchange().expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.correlationId").isEqualTo("request-123");
    }

    @Test
    void fallbackDoesNotThrowWhenCalledWithoutCorrelationHeader() {
        var controller = new FallbackController();
        ReflectionTestUtils.setField(controller, "errorMessage", "Unavailable");
        ReflectionTestUtils.setField(controller, "errorKey", "error");
        ReflectionTestUtils.setField(controller, "correlationIdKey", "correlationId");
        assertEquals(503, controller.fallback(MockServerHttpRequest.get("/fallback").build()).getStatusCode().value());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 405, 413, 429, 502, 503, 504})
    void preservesHttpStatusWithoutDisclosingReason(int status) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/missing"));
        handler.handle(exchange, new ResponseStatusException(HttpStatus.valueOf(status), "secret@database.internal")).block();
        assertEquals(status, exchange.getResponse().getStatusCode().value());
        String body = exchange.getResponse().getBodyAsString().block();
        assertFalse(body.contains("database.internal"));
        assertFalse(body.contains("secret"));
        assertNotNull(exchange.getResponse().getHeaders().getFirst("X-Correlation-Id"));
    }

    @Test
    void preservesAllowHeader() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/missing"));
        handler.handle(exchange, new MethodNotAllowedException("POST", List.of(HttpMethod.GET))).block();
        assertTrue(exchange.getResponse().getHeaders().getAllow().contains(HttpMethod.GET));
    }

    @Test
    void unexpectedFailureHasSafeBodyAndExistingCorrelation() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test"));
        exchange.getResponse().getHeaders().set("X-Correlation-Id", "known-id");
        handler.handle(exchange, new IllegalStateException("internal-password")).block();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertFalse(body.contains("internal-password"));
        assertTrue(body.contains("known-id"));
    }

    @Test
    void committedResponseIsNotRewritten() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test"));
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        exchange.getResponse().setComplete().block();
        var failure = new IllegalStateException("already committed");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> handler.handle(exchange, failure).block()));
        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }
}
