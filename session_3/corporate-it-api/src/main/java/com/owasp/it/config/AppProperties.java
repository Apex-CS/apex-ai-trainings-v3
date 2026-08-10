package com.owasp.it.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String workspaceRoot,
        RestartProperties restart,
        List<ManagedServerProperties> servers) {

    public record RestartProperties(int startupTimeoutSeconds) {
    }

    public record ManagedServerProperties(
            String appName,
            String appHost,
            String ownerArea,
            int port,
            String moduleDir) {
    }
}
