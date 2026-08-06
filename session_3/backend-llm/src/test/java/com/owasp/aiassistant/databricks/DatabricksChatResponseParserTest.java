package com.owasp.aiassistant.databricks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabricksChatResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesStringContentFromServingEndpoint() throws Exception {
        AssistantMessage message = parse("""
                {
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "Yes, bring an umbrella."
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """);

        assertEquals("Yes, bring an umbrella.", message.getText());
    }

    @Test
    void parsesStructuredContentFromReasoningModel() throws Exception {
        AssistantMessage message = parse("""
                {
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": [
                          {
                            "type": "reasoning",
                            "summary": [
                              {
                                "type": "summary_text",
                                "text": "Internal reasoning should be ignored."
                              }
                            ]
                          },
                          {
                            "type": "text",
                            "text": "Yes, since it is raining, you should bring an umbrella."
                          }
                        ]
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """);

        assertEquals("Yes, since it is raining, you should bring an umbrella.", message.getText());
    }

    @Test
    void joinsMultipleTextBlocks() throws Exception {
        AssistantMessage message = parse("""
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": [
                          { "type": "text", "text": "First paragraph." },
                          { "type": "text", "text": "Second paragraph." }
                        ]
                      }
                    }
                  ]
                }
                """);

        assertEquals("First paragraph.\nSecond paragraph.", message.getText());
    }

    @Test
    void parsesToolCallsWithStructuredContent() throws Exception {
        AssistantMessage message = parse("""
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": [
                          { "type": "reasoning", "summary": [] },
                          { "type": "text", "text": "Let me check the database." }
                        ],
                        "tool_calls": [
                          {
                            "id": "call_1",
                            "type": "function",
                            "function": {
                              "name": "queryDatabase",
                              "arguments": "{\\"sql\\":\\"select 1\\"}"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
                """);

        assertEquals("Let me check the database.", message.getText());
        assertTrue(message.hasToolCalls());
        assertEquals("queryDatabase", message.getToolCalls().getFirst().name());
    }

    @Test
    void fallsBackToPredictionsFormat() throws Exception {
        AssistantMessage message = parse("""
                {
                  "predictions": ["Legacy response"]
                }
                """);

        assertEquals("Legacy response", message.getText());
    }

    private AssistantMessage parse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        return DatabricksChatResponseParser.parseAssistantMessage(root);
    }
}
