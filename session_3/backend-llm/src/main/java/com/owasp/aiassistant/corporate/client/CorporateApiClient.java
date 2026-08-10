package com.owasp.aiassistant.corporate.client;

import com.owasp.aiassistant.corporate.auth.CorporateApiAuthContext;
import com.owasp.aiassistant.corporate.auth.DemoUserTokenProvider;
import com.owasp.aiassistant.corporate.config.CorporateApiProperties;
import com.owasp.aiassistant.exception.ToolConnectivityException;
import com.owasp.aiassistant.policy.PolicyViolationTracker;
import com.owasp.aiassistant.tools.ToolErrorClassifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

@Component
public class CorporateApiClient {

    private final WebClient webClient;
    private final CorporateApiProperties properties;
    private final CorporateApiAuthContext authContext;
    private final DemoUserTokenProvider tokenProvider;
    private final PolicyViolationTracker policyViolationTracker;

    public CorporateApiClient(
            WebClient.Builder webClientBuilder,
            CorporateApiProperties properties,
            CorporateApiAuthContext authContext,
            DemoUserTokenProvider tokenProvider,
            PolicyViolationTracker policyViolationTracker) {
        this.webClient = webClientBuilder.build();
        this.properties = properties;
        this.authContext = authContext;
        this.tokenProvider = tokenProvider;
        this.policyViolationTracker = policyViolationTracker;
    }

    public String get(String toolName, String baseUrl, String path, Map<String, String> queryParams) {
        try {
            return webClient.get()
                    .uri(baseUrl, uriBuilder -> {
                        var builder = uriBuilder.path(path);
                        queryParams.forEach(builder::queryParam);
                        return builder.build();
                    })
                    .headers(this::applyAuth)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));
        } catch (WebClientResponseException ex) {
            recordForbiddenToolAttempt(toolName, ex);
            return formatApiError(ex);
        } catch (Exception ex) {
            return handleConnectivity(toolName, ex);
        }
    }

    public String put(String toolName, String baseUrl, String path, String jsonBody) {
        try {
            return webClient.put()
                    .uri(baseUrl + path)
                    .headers(this::applyAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));
        } catch (WebClientResponseException ex) {
            recordForbiddenToolAttempt(toolName, ex);
            return formatApiError(ex);
        } catch (Exception ex) {
            return handleConnectivity(toolName, ex);
        }
    }

    public String post(String toolName, String baseUrl, String path, String jsonBody) {
        try {
            return webClient.post()
                    .uri(baseUrl + path)
                    .headers(this::applyAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(120));
        } catch (WebClientResponseException ex) {
            recordForbiddenToolAttempt(toolName, ex);
            return formatApiError(ex);
        } catch (Exception ex) {
            return handleConnectivity(toolName, ex);
        }
    }

    public String financialBaseUrl() {
        return properties.financialBaseUrl();
    }

    public String itBaseUrl() {
        return properties.itBaseUrl();
    }

    public String salesBaseUrl() {
        return properties.salesBaseUrl();
    }

    private void applyAuth(HttpHeaders headers) {
        headers.setBearerAuth(tokenProvider.resolveToken(authContext.get()));
    }

    private void recordForbiddenToolAttempt(String toolName, WebClientResponseException ex) {
        if (ex.getStatusCode().value() == 403) {
            policyViolationTracker.recordHard(
                    CorporateApiPolicyMessages.forbiddenToolMessage(toolName),
                    toolName);
        }
    }

    private static String formatApiError(WebClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            return "Corporate API error (HTTP " + ex.getStatusCode().value() + "): " + body;
        }
        return "Corporate API error (HTTP " + ex.getStatusCode().value() + "): " + ex.getStatusText();
    }

    private String handleConnectivity(String toolName, Exception ex) {
        if (ToolErrorClassifier.isConnectivityError(ex)) {
            throw new ToolConnectivityException(toolName, "Corporate API call failed: " + ex.getMessage(), ex);
        }
        return "Corporate API call failed: " + ex.getMessage();
    }
}
