package com.workshop.mcp.module02.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCMessage;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * MCP client transport that discovers the message endpoint via a single SSE GET request,
 * then sends each JSON-RPC message via HTTP POST and reads the response from the HTTP body.
 *
 * <p>This matches WireMock's stub behaviour: the SSE endpoint closes after sending the
 * {@code event: endpoint} event, and each POST to the message endpoint returns the
 * JSON-RPC response synchronously in the HTTP response body.
 */
class HttpPostMcpTransport implements McpClientTransport {

    private final String baseUrl;
    private final String sseEndpoint;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final AtomicReference<String> messageEndpoint = new AtomicReference<>();
    private volatile Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> incomingHandler;

    HttpPostMcpTransport(String baseUrl, String sseEndpoint, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.sseEndpoint = sseEndpoint;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public Mono<Void> connect(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
        this.incomingHandler = handler;
        return Mono.fromCallable(this::discoverEndpoint)
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * GETs the SSE endpoint and reads line-by-line until the {@code data:} line is found.
     * Falls back to {@code /mcp/message} if the SSE connection closes before discovery
     * (WireMock closes the chunked stream without a final empty-chunk terminator).
     */
    private Void discoverEndpoint() {
        try {
            var request = HttpRequest.newBuilder(URI.create(baseUrl + sseEndpoint))
                    .header("Accept", "text/event-stream")
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var reader = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        messageEndpoint.set(line.substring(5).trim());
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
            // Connection may close abruptly after the endpoint event — that is expected.
        }
        // If the SSE data line was never received, fall back to the standard MCP path.
        messageEndpoint.compareAndSet(null, "/mcp/message");
        return null;
    }

    @Override
    public Mono<Void> sendMessage(JSONRPCMessage message) {
        return Mono.fromCallable(() -> {
            String json = objectMapper.writeValueAsString(message);
            var request = HttpRequest.newBuilder(URI.create(baseUrl + messageEndpoint.get()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (response.statusCode() == 200 && body != null && !body.isBlank()) {
                JSONRPCMessage reply = McpSchema.deserializeJsonRpcMessage(objectMapper, body);
                incomingHandler.apply(Mono.just(reply)).subscribe();
            }
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.empty();
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
        return objectMapper.convertValue(data, typeRef);
    }
}
