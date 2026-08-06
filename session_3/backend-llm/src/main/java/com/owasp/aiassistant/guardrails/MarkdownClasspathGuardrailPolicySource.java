package com.owasp.aiassistant.guardrails;

import com.owasp.aiassistant.config.GuardrailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.guardrails.enabled", havingValue = "true", matchIfMissing = true)
public class MarkdownClasspathGuardrailPolicySource implements GuardrailPolicySource {

    private static final Logger log = LoggerFactory.getLogger(MarkdownClasspathGuardrailPolicySource.class);

    private final GuardrailProperties properties;
    private final ResourceLoader resourceLoader;

    public MarkdownClasspathGuardrailPolicySource(
            GuardrailProperties properties,
            ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public String getSourceName() {
        return "markdown-classpath";
    }

    @Override
    public List<GuardrailPolicy> loadPolicies() {
        List<GuardrailPolicy> policies = new ArrayList<>();
        String baseLocation = normalizeLocation(properties.getMarkdown().getLocation());

        for (Map.Entry<String, GuardrailType> entry : properties.getMarkdown().getFiles().entrySet()) {
            String fileStem = entry.getKey();
            GuardrailType type = entry.getValue();
            String resourcePath = baseLocation + fileStem + ".md";
            Resource resource = resourceLoader.getResource(resourcePath);

            if (!resource.exists()) {
                log.warn("Guardrail markdown file not found, skipping: {}", resourcePath);
                continue;
            }

            try {
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                GuardrailEnforcement enforcement = resolveEnforcement(type);
                policies.add(new GuardrailPolicy(
                        fileStem,
                        humanize(fileStem),
                        type,
                        enforcement,
                        content,
                        resourcePath));
            } catch (IOException e) {
                log.error("Failed to load guardrail markdown file: {}", resourcePath, e);
            }
        }

        return List.copyOf(policies);
    }

    private static GuardrailEnforcement resolveEnforcement(GuardrailType type) {
        return switch (type) {
            case INPUT -> GuardrailEnforcement.HARD;
            case SYSTEM, OUTPUT, TOOL, JUDGE -> GuardrailEnforcement.SOFT;
        };
    }

    private static String normalizeLocation(String location) {
        if (location.endsWith("/")) {
            return location;
        }
        return location + "/";
    }

    private static String humanize(String fileStem) {
        return fileStem.replace('-', ' ');
    }
}
