package com.donornexa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the Spring application context loads successfully
 * with all Phase 2 entities, repositories, and configurations.
 *
 * Uses the test/resources/application.properties which points to an
 * H2 in-memory database so this test runs without a live MySQL instance.
 */
@SpringBootTest
@ActiveProfiles("test")
class DonorNexaApplicationTests {

    @Test
    void contextLoads() {
        // If this passes, the application context — including all JPA entities
        // and Spring Data repositories — wired correctly.
    }
}
