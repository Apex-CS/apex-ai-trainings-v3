package com.workshop.concierge.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds a couple of demo long-term preferences so /meal-plan/generate and
 * /memory/preferences/{userId} can be exercised without a prior write call.
 */
@Component
public class DemoPreferenceSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoPreferenceSeeder.class);
    private static final String DEMO_USER_ID = "demo-user";

    private final LongTermMemoryService longTermMemoryService;

    public DemoPreferenceSeeder(LongTermMemoryService longTermMemoryService) {
        this.longTermMemoryService = longTermMemoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            longTermMemoryService.savePreference(DEMO_USER_ID, "Allergic to peanuts");
            longTermMemoryService.savePreference(DEMO_USER_ID, "Allergic to shellfish");
            longTermMemoryService.savePreference(DEMO_USER_ID, "Prefers high-protein, low-carb meals");
            log.info("[DemoPreferenceSeeder] seeded long-term preferences for userId={}", DEMO_USER_ID);
        } catch (Exception e) {
            // Don't let a missing/invalid GEMINI_API_KEY at startup prevent the app (and UI) from booting.
            log.warn("[DemoPreferenceSeeder] skipped seeding demo preferences: {}", e.getMessage());
        }
    }
}
