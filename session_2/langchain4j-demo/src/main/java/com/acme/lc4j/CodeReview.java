package com.acme.lc4j;

import java.util.List;

/** Same domain object as the Spring AI demo - to show the CONCEPTS are identical. */
public record CodeReview(String summary, String overallSeverity, List<String> issues, int qualityScore) {}
