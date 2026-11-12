package com.example.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.secret=test-only-key-with-at-least-32-bytes-long")
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
