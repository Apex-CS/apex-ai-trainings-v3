package com.workshop.mcp.module02.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO representing a Jira Issue deserialized from the MCP tool response.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} is mandatory:
 * Jira issues have dozens of optional fields. The MCP server may return
 * a subset, and future server versions may add new fields. Without this
 * annotation, Jackson throws on unrecognised fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssueDTO(

        @JsonProperty("key")
        String key,

        @JsonProperty("summary")
        String summary,

        @JsonProperty("status")
        String status,

        @JsonProperty("priority")
        String priority,

        @JsonProperty("issuetype")
        String issueType,

        @JsonProperty("assignee")
        String assignee,

        @JsonProperty("reporter")
        String reporter,

        @JsonProperty("created")
        String created,

        @JsonProperty("updated")
        String updated,

        @JsonProperty("fixVersions")
        List<String> fixVersions,

        @JsonProperty("labels")
        List<String> labels
) {
    /**
     * Business rule: is this a release blocker?
     * Used in Module 05 to gate deployments.
     */
    public boolean isReleaseBlocker() {
        return "Critical".equalsIgnoreCase(priority)
                && "Bug".equalsIgnoreCase(issueType)
                && !"Done".equalsIgnoreCase(status)
                && !"Closed".equalsIgnoreCase(status);
    }
}
