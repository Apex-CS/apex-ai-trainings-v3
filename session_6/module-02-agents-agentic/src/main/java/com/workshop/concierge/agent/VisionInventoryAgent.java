package com.workshop.concierge.agent;

import java.util.List;

import com.workshop.concierge.dto.InventoryScanResult;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Vision &amp; Inventory Agent — extracts a structured inventory list (with quantities and
 * estimated expiration dates) from a pantry/fridge photo and/or a raw text item list.
 */
public interface VisionInventoryAgent {

    @SystemMessage("""
            You are the Vision & Inventory Agent of the Kitchen Concierge system.
            You analyze photos of fridges/pantries or free-text lists of items and extract a
            structured inventory: item name, quantity (best estimate, e.g. "2", "1 bag", "500g"),
            an estimated expiration window (e.g. "3-5 days", "2 weeks", "unknown" if not perishable
            information is visible), and a category (produce, dairy, meat, pantry, beverage, other).
            If you see a receipt, IGNORE any address, phone number, card number or payment details —
            only extract food/grocery item names and quantities.
            Always respond with a complete, valid structured result.
            """)
        String analyzeImage(@UserMessage List<Content> contents);

    @SystemMessage("""
            You are the Vision & Inventory Agent of the Kitchen Concierge system.
            You analyze a free-text list of pantry/fridge items and extract a structured inventory:
            item name, quantity, an estimated expiration window, and a category
            (produce, dairy, meat, pantry, beverage, other).
            """)
    @UserMessage("""
            Extract a structured inventory from the following text item list:
            {{items}}
            """)
    InventoryScanResult analyzeText(@V("items") String items);
}
