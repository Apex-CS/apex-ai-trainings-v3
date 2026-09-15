package com.workshop.concierge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.workshop.concierge.agent.NutritionistAgent;
import com.workshop.concierge.agent.RecipeGroceryAgent;
import com.workshop.concierge.agent.VisionInventoryAgent;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;

/**
 * Wires each specialized {@code @AiService} agent interface to the shared chat model,
 * short-term chat memory and mock tools.
 */
@Configuration
public class AiServiceConfig {

    @Bean
    public VisionInventoryAgent visionInventoryAgent(ChatLanguageModel chatLanguageModel) {
        // Stateless: every scan is an independent extraction, no conversational memory needed.
        return AiServices.create(VisionInventoryAgent.class, chatLanguageModel);
    }

    @Bean
    public NutritionistAgent nutritionistAgent(ChatLanguageModel chatLanguageModel,
                                                ChatMemoryProvider chatMemoryProvider) {
        return AiServices.builder(NutritionistAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    @Bean
    public RecipeGroceryAgent recipeGroceryAgent(ChatLanguageModel chatLanguageModel,
                              ChatMemoryProvider chatMemoryProvider) {
        return AiServices.builder(RecipeGroceryAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }
}
