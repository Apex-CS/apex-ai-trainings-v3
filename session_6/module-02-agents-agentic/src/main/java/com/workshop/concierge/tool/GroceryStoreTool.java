package com.workshop.concierge.tool;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.Tool;

/**
 * Mock grocery store integration exposed to the LLM as a {@code @Tool}.
 * In a real system this would call an external grocery/e-commerce API.
 */
@Component
public class GroceryStoreTool {

    private static final Logger log = LoggerFactory.getLogger(GroceryStoreTool.class);

    private static final Map<String, Double> KNOWN_PRICES = Map.of(
            "chicken breast", 6.99,
            "brown rice", 2.49,
            "broccoli", 1.99,
            "eggs", 3.49,
            "greek yogurt", 4.29,
            "spinach", 2.19,
            "salmon", 9.99,
            "almond milk", 3.79
    );

    @Tool("Checks the current price and stock availability of a grocery item by name. " +
            "Returns a short string with price and stock status.")
    public String checkItemPriceAndStock(String item) {
        String key = item == null ? "" : item.trim().toLowerCase(Locale.ROOT);
        double price = KNOWN_PRICES.getOrDefault(key, roundToCents(ThreadLocalRandom.current().nextDouble(1.5, 12.0)));
        boolean inStock = ThreadLocalRandom.current().nextInt(100) < 85;
        String result = "item=%s, price=$%.2f, inStock=%s".formatted(item, price, inStock);
        log.info("[GroceryStoreTool] {}", result);
        return result;
    }

    private double roundToCents(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
