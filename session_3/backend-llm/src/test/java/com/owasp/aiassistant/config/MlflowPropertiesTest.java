package com.owasp.aiassistant.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MlflowPropertiesTest {

    @Test
    void isDatabricks_whenTrackingUriIsDatabricks() {
        MlflowProperties properties = new MlflowProperties();
        properties.setTrackingUri("databricks");

        assertTrue(properties.isDatabricks());
    }

    @Test
    void isDatabricks_whenTrackingUriIsLocalHttpUrl() {
        MlflowProperties properties = new MlflowProperties();
        properties.setTrackingUri("http://localhost:5001");

        assertFalse(properties.isDatabricks());
    }

    @Test
    void resolveApiBaseUrl_usesDatabricksHostForDatabricksBackend() {
        MlflowProperties properties = new MlflowProperties();
        properties.setTrackingUri("databricks");

        assertEquals(
                "https://dbc.example.cloud.databricks.com",
                properties.resolveApiBaseUrl("https://dbc.example.cloud.databricks.com/"));
    }

    @Test
    void resolveApiBaseUrl_usesTrackingUriForLocalBackend() {
        MlflowProperties properties = new MlflowProperties();
        properties.setTrackingUri("http://localhost:5001");

        assertEquals("http://localhost:5001", properties.resolveApiBaseUrl(null));
    }

    @Test
    void resolveApiBaseUrl_requiresDatabricksHostForDatabricksBackend() {
        MlflowProperties properties = new MlflowProperties();
        properties.setTrackingUri("databricks");

        assertThrows(IllegalStateException.class, () -> properties.resolveApiBaseUrl(""));
    }
}
