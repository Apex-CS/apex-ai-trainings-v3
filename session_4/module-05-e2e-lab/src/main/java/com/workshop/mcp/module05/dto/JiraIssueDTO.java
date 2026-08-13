package com.workshop.mcp.module05.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssueDTO(
        @JsonProperty("key")       String key,
        @JsonProperty("summary")   String summary,
        @JsonProperty("status")    String status,
        @JsonProperty("priority")  String priority,
        @JsonProperty("issuetype") String issueType,
        @JsonProperty("fixVersions") List<String> fixVersions
) {}
