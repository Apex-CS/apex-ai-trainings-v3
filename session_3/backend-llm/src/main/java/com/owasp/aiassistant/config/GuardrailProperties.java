package com.owasp.aiassistant.config;

import com.owasp.aiassistant.guardrails.GuardrailType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.guardrails")
public class GuardrailProperties {

    private boolean enabled = true;
    private Markdown markdown = new Markdown();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Markdown getMarkdown() {
        return markdown;
    }

    public void setMarkdown(Markdown markdown) {
        this.markdown = markdown;
    }

    public static class Markdown {

        private String location = "classpath:guardrails/";
        private Map<String, GuardrailType> files = defaultFiles();

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public Map<String, GuardrailType> getFiles() {
            return files;
        }

        public void setFiles(Map<String, GuardrailType> files) {
            this.files = files;
        }

        private static Map<String, GuardrailType> defaultFiles() {
            Map<String, GuardrailType> defaults = new LinkedHashMap<>();
            defaults.put("system-policy", GuardrailType.SYSTEM);
            defaults.put("tool-usage-policy", GuardrailType.TOOL);
            defaults.put("blocked-topics", GuardrailType.INPUT);
            defaults.put("output-policy", GuardrailType.OUTPUT);
            defaults.put("judge-rubric", GuardrailType.JUDGE);
            return defaults;
        }
    }
}
