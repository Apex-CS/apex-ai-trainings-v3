package com.owasp.aiassistant.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.exception.ToolConnectivityException;
import com.owasp.aiassistant.tools.ToolErrorClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class WebSearchService {

    private static final String TOOL_NAME = "searchWeb";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int maxResults;

    public WebSearchService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.web-search.enabled:true}") boolean enabled,
            @Value("${app.web-search.max-results:5}") int maxResults) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.duckduckgo.com")
                .build();
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.maxResults = maxResults;
    }

    public String search(String query) {
        if (!enabled) {
            return "Web search is disabled.";
        }

        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/")
                            .queryParam("q", query)
                            .queryParam("format", "json")
                            .queryParam("no_redirect", "1")
                            .queryParam("no_html", "1")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isBlank()) {
                return "No web search results found.";
            }

            JsonNode root = objectMapper.readTree(response);
            List<String> snippets = new ArrayList<>();

            appendIfPresent(snippets, "Abstract", root.path("AbstractText").asText(null));
            appendIfPresent(snippets, "Answer", root.path("Answer").asText(null));

            JsonNode related = root.path("RelatedTopics");
            if (related.isArray()) {
                for (JsonNode topic : related) {
                    if (snippets.size() >= maxResults) {
                        break;
                    }
                    String text = topic.path("Text").asText(null);
                    if (text != null && !text.isBlank()) {
                        snippets.add(text);
                    }
                }
            }

            if (snippets.isEmpty()) {
                return "No web search results found for: " + query;
            }

            return String.join("\n\n", snippets);
        } catch (Exception e) {
            if (ToolErrorClassifier.isConnectivityError(e)) {
                throw new ToolConnectivityException(TOOL_NAME, "Web search failed: " + e.getMessage(), e);
            }
            return "Web search failed: " + e.getMessage();
        }
    }

    private static void appendIfPresent(List<String> snippets, String label, String value) {
        if (value != null && !value.isBlank()) {
            snippets.add(label + ": " + value);
        }
    }
}
