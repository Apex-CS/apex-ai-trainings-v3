package com.owasp.aiassistant.config;

import com.owasp.aiassistant.guardrails.GuardrailEnforcement;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.guardrails.output-judge")
public class OutputJudgeProperties {

    private boolean enabled = true;
    private int maxRetries = 1;
    private GuardrailEnforcement enforcement = GuardrailEnforcement.HARD;
    private boolean failOpenOnError = true;
    private Model model = new Model();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public GuardrailEnforcement getEnforcement() {
        return enforcement;
    }

    public void setEnforcement(GuardrailEnforcement enforcement) {
        this.enforcement = enforcement;
    }

    public boolean isFailOpenOnError() {
        return failOpenOnError;
    }

    public void setFailOpenOnError(boolean failOpenOnError) {
        this.failOpenOnError = failOpenOnError;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public static class Model {

        private String host;
        private String token;
        private String endpointName;
        private String chatInvocationUrl;
        private Double temperature = 0.0;
        private Integer maxTokens = 1024;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getEndpointName() {
            return endpointName;
        }

        public void setEndpointName(String endpointName) {
            this.endpointName = endpointName;
        }

        public String getChatInvocationUrl() {
            return chatInvocationUrl;
        }

        public void setChatInvocationUrl(String chatInvocationUrl) {
            this.chatInvocationUrl = chatInvocationUrl;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }
    }
}
