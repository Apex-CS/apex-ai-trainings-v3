package com.owasp.aiassistant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GuardrailProperties.class)
public class GuardrailConfig {
}
