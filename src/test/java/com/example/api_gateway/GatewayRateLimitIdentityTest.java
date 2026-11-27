package com.example.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayRateLimitIdentityTest {

    private String resolve(String user, String path) {
        var jwt = mock(JwtUtil.class);
        when(jwt.validateToken("token")).thenReturn(Mono.just(true));
        when(jwt.extractUsername("token")).thenReturn(Mono.just(user));
        when(jwt.extractRole("token")).thenReturn(Mono.just("USER"));

        var filter = new JwtAuthenticationFilter(jwt);
        ReflectionTestUtils.setField(filter, "userIdHeader", "X-User-Id");
        ReflectionTestUtils.setField(filter, "gatewaySecret", "test-secret");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path)
                .remoteAddress(new InetSocketAddress("127.0.0.1", 1234))
                .header("Authorization", "Bearer token")
                .header("X-User-Id", "forged")
                .header("X-Forwarded-For", "203.0.113.5"));

        var key = new AtomicReference<String>();
        GatewayFilterChain chain = forwarded -> new GatewayConfig().userKeyResolver()
                .resolve(forwarded).doOnNext(key::set).then();
        filter.filter(exchange, chain).block();
        return key.get();
    }

    @Test
    void usersBehindTheSameProxyHaveIndependentBuckets() {
        assertEquals("user:alice", resolve("alice", "/api/v1/users/me"));
        assertEquals("user:bob", resolve("bob", "/api/v1/users/me"));
    }

    @Test
    void publicRequestsCannotForgeAnAuthenticatedBucket() {
        assertEquals("anonymous:127.0.0.1", resolve("alice", "/api/v1/media/example"));
    }
}
