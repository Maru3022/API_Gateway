package com.example.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
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
        assertNull(config.checkOrigin("https://untrusted.example"));
    }
}
