package com.example.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@AutoConfigureWebTestClient
@SpringBootTest(properties = {
        "jwt.secret=test-only-key-with-at-least-32-bytes-long",
        "internal.gateway.secret=test-only-internal-gateway-secret",
        "eureka.client.enabled=false"
})
class ApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {
    }

    @Test
    void actuatorExposesOnlyProbeHealthToUnauthenticatedClients() {
        webTestClient.get().uri("/actuator/health/liveness")
                .exchange().expectStatus().isOk();
        webTestClient.get().uri("/actuator/gateway/routes")
                .exchange().expectStatus().isUnauthorized();
        webTestClient.get().uri("/actuator/prometheus")
                .exchange().expectStatus().isUnauthorized();
    }

}
