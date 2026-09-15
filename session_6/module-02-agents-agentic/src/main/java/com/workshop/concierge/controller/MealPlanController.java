package com.workshop.concierge.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.workshop.concierge.dto.MealPlanRequest;
import com.workshop.concierge.dto.MealPlanResponse;
import com.workshop.concierge.orchestration.SupervisorOrchestrator;

import jakarta.validation.Valid;

/**
 * Nutritionist + Recipe & Grocery Agent orchestration entry point.
 */
@RestController
public class MealPlanController {

    private final SupervisorOrchestrator orchestrator;

    public MealPlanController(SupervisorOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/api/v1/concierge/meal-plan/generate")
    public ResponseEntity<MealPlanResponse> generate(@Valid @RequestBody MealPlanRequest request) {
        return ResponseEntity.ok(orchestrator.generateMealPlan(request));
    }
}
