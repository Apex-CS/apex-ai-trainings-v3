package com.acme.aitraining.controller;

import com.acme.aitraining.dto.RagResponse;
import com.acme.aitraining.service.RagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

  private final RagService ragService;

  public RagController(RagService ragService) {
    this.ragService = ragService;
  }

  @GetMapping
  public RagResponse ask(
      @RequestParam String question) {

    return ragService.ask(question);
  }
}