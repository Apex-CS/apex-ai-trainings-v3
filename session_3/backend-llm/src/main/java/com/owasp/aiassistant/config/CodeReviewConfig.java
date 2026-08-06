package com.owasp.aiassistant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CodeReviewProperties.class)
public class CodeReviewConfig {
}
