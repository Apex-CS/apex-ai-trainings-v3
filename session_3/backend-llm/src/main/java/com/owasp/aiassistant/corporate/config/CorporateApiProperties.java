package com.owasp.aiassistant.corporate.config;

import com.owasp.aiassistant.corporate.enums.DemoUser;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app.corporate-api")
public record CorporateApiProperties(
        String financialBaseUrl,
        String itBaseUrl,
        String salesBaseUrl,
        DemoUser defaultDemoUser,
        Map<String, String> demoTokens) {
}
