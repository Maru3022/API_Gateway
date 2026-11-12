package com.example.api_gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Mono<String> extractUsername(String token) {
        return Mono.fromCallable(() -> extractClaim(token, Claims::getSubject));
    }

    public Mono<String> extractRole(String token) {
        return Mono.fromCallable(() -> extractClaim(token, claims -> claims.get("role", String.class)));
    }

    public Mono<String> extractType(String token) {
        return Mono.fromCallable(() -> extractClaim(token, claims -> claims.get("type", String.class)));
    }

    public Mono<Date> extractExpiration(String token) {
        return Mono.just(extractClaim(token, Claims::getExpiration));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Boolean> isTokenExpired(String token) {
        return extractExpiration(token)
                .map(expiration -> expiration.before(new Date()));
    }

    public Mono<Boolean> validateToken(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = extractAllClaims(token);
                return claims.getSubject() != null && !claims.getSubject().isBlank()
                        && "access".equals(claims.get("type", String.class))
                        && claims.getExpiration() != null
                        && claims.getExpiration().after(new Date());
            } catch (JwtException | IllegalArgumentException exception) {
                return false;
            }
        });
    }
}
