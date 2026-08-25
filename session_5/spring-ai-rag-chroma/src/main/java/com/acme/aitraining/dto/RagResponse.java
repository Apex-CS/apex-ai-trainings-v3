package com.acme.aitraining.dto;

import java.util.List;

public record RagResponse(
  String question,
  List<Float> embedding,
  List<String> retrievedDocuments,
  String prompt,
  String answer
) {}