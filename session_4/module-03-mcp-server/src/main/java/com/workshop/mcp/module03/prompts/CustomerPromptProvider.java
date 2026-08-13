package com.workshop.mcp.module03.prompts;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * MCP Prompt Provider — Module 03.
 *
 * <p>Prompts are parameterized message templates. The LLM calls prompts/get,
 * fills in the arguments, and uses the returned messages as its system/user context.
 * This is more powerful than hardcoding prompts in the client — the server controls
 * the prompt logic and can update it without client changes.
 *
 * <p>Registered prompts:
 * <ul>
 *   <li>{@code customer_support_response} — generates a professional support reply</li>
 *   <li>{@code customer_onboarding_email} — welcome email template</li>
 * </ul>
 */
@Configuration
public class CustomerPromptProvider {

    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> customerPrompts() {
        return List.of(
                supportResponsePrompt(),
                onboardingEmailPrompt()
        );
    }

    private McpServerFeatures.SyncPromptSpecification supportResponsePrompt() {
        var prompt = new McpSchema.Prompt(
                "customer_support_response",
                "Generates a professional customer support response email",
                List.of(
                        new McpSchema.PromptArgument("customer_name", "Customer's full name", true),
                        new McpSchema.PromptArgument("issue_summary", "Brief description of the customer's issue", true),
                        new McpSchema.PromptArgument("resolution", "Resolution or next steps taken (optional)", false)
                ));

        return new McpServerFeatures.SyncPromptSpecification(prompt, (McpSyncServerExchange exchange, McpSchema.GetPromptRequest req) -> {
            Map<String, Object> args = req.arguments() != null ? req.arguments() : Map.of();
            String customerName = String.valueOf(args.getOrDefault("customer_name", "Valued Customer"));
            String issueSummary = String.valueOf(args.getOrDefault("issue_summary", "your recent inquiry"));
            Object resolutionObj = args.get("resolution");
            String resolution = resolutionObj != null ? String.valueOf(resolutionObj) : null;

            String systemContent = """
                    You are a professional customer support agent for a SaaS platform.
                    Always be empathetic, clear, and solution-oriented.
                    Use formal but friendly language. Sign off as "Customer Success Team".""";

            String userContent = """
                    Write a customer support response email for %s.

                    Issue reported: %s

                    %s

                    The email should:
                    1. Acknowledge the issue with empathy
                    2. Clearly explain the resolution or current status
                    3. Provide a next step or timeline
                    4. End with an offer for further assistance""".formatted(
                    customerName,
                    issueSummary,
                    resolution != null
                            ? "Resolution applied: " + resolution
                            : "Status: Under active investigation — ETA for resolution: 24 hours");

            return new McpSchema.GetPromptResult(
                    "Customer support response prompt",
                    List.of(
                            new McpSchema.PromptMessage(
                                    McpSchema.Role.ASSISTANT,
                                    new McpSchema.TextContent(systemContent)),
                            new McpSchema.PromptMessage(
                                    McpSchema.Role.USER,
                                    new McpSchema.TextContent(userContent))));
        });
    }

    private McpServerFeatures.SyncPromptSpecification onboardingEmailPrompt() {
        var prompt = new McpSchema.Prompt(
                "customer_onboarding_email",
                "Generates a welcome / onboarding email for a new customer",
                List.of(
                        new McpSchema.PromptArgument("customer_name", "New customer's full name", true),
                        new McpSchema.PromptArgument("tier", "Service tier: STANDARD, PREMIUM, or ENTERPRISE", true)
                ));

        return new McpServerFeatures.SyncPromptSpecification(prompt, (McpSyncServerExchange exchange, McpSchema.GetPromptRequest req) -> {
            Map<String, Object> args = req.arguments() != null ? req.arguments() : Map.of();
            String customerName = String.valueOf(args.getOrDefault("customer_name", "New Customer"));
            String tier = String.valueOf(args.getOrDefault("tier", "STANDARD"));

            String systemContent = "You are a customer success manager writing onboarding emails. Be enthusiastic and helpful.";

            String userContent = """
                    Write a welcome onboarding email for %s who just signed up for the %s plan.
                    Include: account activation steps, key features for their tier, and support contact info."""
                    .formatted(customerName, tier);

            return new McpSchema.GetPromptResult(
                    "Customer onboarding email prompt",
                    List.of(
                            new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT,
                                    new McpSchema.TextContent(systemContent)),
                            new McpSchema.PromptMessage(McpSchema.Role.USER,
                                    new McpSchema.TextContent(userContent))));
        });
    }
}
