package com.keystone;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the application context loads successfully.
 * Uses H2 in-memory DB (see src/test/resources/application.properties).
 */
@SpringBootTest
@ActiveProfiles("test")
class KeystoneBackendApplicationTests {

    @Test
    void contextLoads() {
        // passes if Spring context assembles without errors
    }
}
