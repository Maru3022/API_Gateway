package com.example.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;

class JwtAuthenticationFilterTest {
    @Test
    void gatewayAddsInternalSecretAndRemovesClientSuppliedValue() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/auth/login")
                .header("X-Internal-Gateway-Secret", "forged"));
        var forwarded = new AtomicReference<ServerWebExchange>();
        GatewayFilterChain chain = request -> {
            forwarded.set(request);
            return reactor.core.publisher.Mono.empty();
        };
        var filter = new JwtAuthenticationFilter(mock(JwtUtil.class));
        ReflectionTestUtils.setField(filter, "gatewaySecret", "test-gateway-secret");

        filter.filter(exchange, chain).block();

        assertEquals("test-gateway-secret", forwarded.get().getRequest().getHeaders()
                .getFirst("X-Internal-Gateway-Secret"));
    }

    @Test
    void forgedIdentityWithoutTokenIsNotForwarded() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/123")
                .header("X-User-Id", "123"));
        var chain = mock(GatewayFilterChain.class);
        new JwtAuthenticationFilter(mock(JwtUtil.class)).filter(exchange, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(chain);
    }

    @Test
    void malformedTokenIsRejectedWithoutThrowing() {
        var jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "test-only-key-with-at-least-32-bytes-long");
        assertFalse(jwt.validateToken("not-a-jwt").block());
    }

    @Test
    void credentialedCorsHasValidOrigins() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users"));
        var config = new SecurityConfig().corsConfigurationSource().getCorsConfiguration(exchange);
        assertNotNull(config);
        assertDoesNotThrow(config::validateAllowCredentials);
        assertEquals("http://127.0.0.1:5173", config.checkOrigin("http://127.0.0.1:5173"));
        assertEquals("http://localhost:5173", config.checkOrigin("http://localhost:5173"));
        assertNull(config.checkOrigin("https://untrusted.example"));
    }
}
