package com.owasp.aiassistant.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutputJudgeConfigTest {

    @Test
    void fallsBackToMainDatabricksSettingsWhenJudgeModelIsUnset() {
        DatabricksProperties main = new DatabricksProperties();
        main.setHost("https://main.example");
        main.setToken("main-token");
        main.setEndpointName("main-endpoint");
        main.setTemperature(0.2);
        main.setMaxTokens(4096);

        OutputJudgeProperties judge = new OutputJudgeProperties();

        DatabricksProperties resolved = OutputJudgeConfig.resolveJudgeProperties(main, judge);

        assertEquals("https://main.example", resolved.getHost());
        assertEquals("main-token", resolved.getToken());
        assertEquals("main-endpoint", resolved.getEndpointName());
        assertEquals(0.0, resolved.getTemperature());
        assertEquals(1024, resolved.getMaxTokens());
    }

    @Test
    void usesJudgeModelOverridesWhenProvided() {
        DatabricksProperties main = new DatabricksProperties();
        main.setHost("https://main.example");
        main.setToken("main-token");
        main.setEndpointName("main-endpoint");

        OutputJudgeProperties judge = new OutputJudgeProperties();
        judge.getModel().setHost("https://judge.example");
        judge.getModel().setToken("judge-token");
        judge.getModel().setEndpointName("judge-endpoint");
        judge.getModel().setTemperature(0.1);
        judge.getModel().setMaxTokens(512);

        DatabricksProperties resolved = OutputJudgeConfig.resolveJudgeProperties(main, judge);

        assertEquals("https://judge.example", resolved.getHost());
        assertEquals("judge-token", resolved.getToken());
        assertEquals("judge-endpoint", resolved.getEndpointName());
        assertEquals(0.1, resolved.getTemperature());
        assertEquals(512, resolved.getMaxTokens());
    }
}
