package com.owasp.aiassistant.corporate.auth;

import com.owasp.aiassistant.corporate.config.CorporateApiProperties;
import com.owasp.aiassistant.corporate.enums.DemoUser;
import org.springframework.stereotype.Component;

@Component
public class DemoUserTokenProvider {

    private final CorporateApiProperties properties;

    public DemoUserTokenProvider(CorporateApiProperties properties) {
        this.properties = properties;
    }

    public String resolveToken(DemoUser demoUser) {
        DemoUser effectiveUser = demoUser != null ? demoUser : properties.defaultDemoUser();
        String token = properties.demoTokens().get(effectiveUser.name());
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No demo JWT configured for user: " + effectiveUser.username());
        }
        return token;
    }
}
