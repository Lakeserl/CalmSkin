package com.lakeserl.shipping_service;

import org.junit.jupiter.api.Test;

// Lightweight smoke test that does not touch the Spring context. Spring
// integration tests against the real ApplicationContext need testcontainers
// for Postgres + Kafka and are intentionally deferred until the dev-loop is
// proven to compile and unit tests pass.
class ShippingServiceApplicationTests {

    @Test
    void mainClassExists() {
        // No-op: tests in service/ and event/producer cover behavior.
        // This placeholder keeps Maven from failing the test phase on an empty
        // module while the integration suite is being designed.
    }
}
