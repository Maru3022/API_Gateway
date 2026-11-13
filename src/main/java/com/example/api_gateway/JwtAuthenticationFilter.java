package com.example.api_gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Value("${gateway.jwt.user-id-header}")
    private String userIdHeader;

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())
                || path.startsWith("/api/v1/auth/")
                || (HttpMethod.GET.equals(exchange.getRequest().getMethod())
                    && path.matches("/api/v1/users/[0-9a-fA-F-]+/avatar"))
                || (HttpMethod.GET.equals(exchange.getRequest().getMethod())
                    && path.startsWith("/api/v1/media/"))
                || path.startsWith("/actuator/")) {
            return chain.filter(withoutIdentityHeaders(exchange));
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        return jwtUtil.validateToken(token)
                .flatMap(valid -> {
                    if (valid) {
                        return Mono.zip(jwtUtil.extractUsername(token), jwtUtil.extractRole(token))
                                .onErrorResume(JwtException.class, error -> Mono.empty())
                                .switchIfEmpty(Mono.defer(() -> {
                                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                    return exchange.getResponse().setComplete().then(Mono.<reactor.util.function.Tuple2<String, String>>empty());
                                }))
                                .flatMap(identity -> {
                                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                            .headers(headers -> {
                                                headers.remove(userIdHeader);
                                                headers.remove(USER_ROLE_HEADER);
                                                headers.set(userIdHeader, identity.getT1());
                                                headers.set(USER_ROLE_HEADER, identity.getT2());
                                            })
                                            .build();
                                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                                });
                    } else {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                });
    }

    private ServerWebExchange withoutIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(userIdHeader);
                    headers.remove(USER_ROLE_HEADER);
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
