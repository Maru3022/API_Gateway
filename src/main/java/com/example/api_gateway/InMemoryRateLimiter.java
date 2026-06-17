package com.example.api_gateway;

import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryRateLimiter implements RateLimiter<InMemoryRateLimiter.Config> {

    private final Config config;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(int replenishRate, int burstCapacity) {
        this.config = new Config(replenishRate, burstCapacity);
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        TokenBucket bucket = buckets.computeIfAbsent(id, key -> new TokenBucket(config.getBurstCapacity(), Instant.now()));
        boolean allowed;

        synchronized (bucket) {
            refill(bucket);
            if (bucket.tokens.get() > 0) {
                bucket.tokens.decrementAndGet();
                allowed = true;
            } else {
                allowed = false;
            }
        }

        if (allowed) {
            return Mono.just(new Response(true, Collections.emptyMap()));
        }
        return Mono.just(new Response(false, Map.of("X-Rate-Limit-Denied", "true")));
    }

    @Override
    public Map<String, Config> getConfig() {
        return Map.of("default", this.config);
    }

    @Override
    public Config newConfig() {
        return new Config(config.getReplenishRate(), config.getBurstCapacity());
    }

    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    private void refill(TokenBucket bucket) {
        Instant now = Instant.now();
        long elapsedSeconds = Duration.between(bucket.lastRefill, now).getSeconds();
        if (elapsedSeconds <= 0) {
            return;
        }

        int tokensToAdd = (int) (elapsedSeconds * config.getReplenishRate());
        if (tokensToAdd > 0) {
            int newTokens = Math.min(config.getBurstCapacity(), bucket.tokens.get() + tokensToAdd);
            bucket.tokens.set(newTokens);
            bucket.lastRefill = now;
        }
    }

    public static class Config {
        private int replenishRate;
        private int burstCapacity;

        public Config(int replenishRate, int burstCapacity) {
            this.replenishRate = replenishRate;
            this.burstCapacity = burstCapacity;
        }

        public int getReplenishRate() {
            return replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }
    }

    private static class TokenBucket {
        private final AtomicInteger tokens;
        private Instant lastRefill;

        TokenBucket(int initialTokens, Instant createdAt) {
            this.tokens = new AtomicInteger(initialTokens);
            this.lastRefill = createdAt;
        }
    }
}
