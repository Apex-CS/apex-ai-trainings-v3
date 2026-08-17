package com.workshop.mcp.jira.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Jira MCP Tools — mock implementation for Module 05 end-to-end lab.
 *
 * <p>Returns fixture data matching the scenarios described in the README:
 * <ul>
 *   <li>Release 2.4 → 2 critical open bugs (PROJ-101, PROJ-108)</li>
 *   <li>Release 3.0 → no critical bugs</li>
 * </ul>
 */
@Service
public class JiraTools {

    private static final String BUGS_2_4 = """
            [
              {
                "key": "PROJ-101",
                "summary": "NPE in payment processor when card token is null",
                "status": "Open",
                "priority": "Critical",
                "issuetype": "Bug",
                "fixVersions": ["2.4"]
              },
              {
                "key": "PROJ-108",
                "summary": "Deadlock in session management under high concurrency",
                "status": "In Progress",
                "priority": "Critical",
                "issuetype": "Bug",
                "fixVersions": ["2.4"]
              }
            ]""";

    @Tool(description = """
            Searches Jira issues using a JQL (Jira Query Language) query string.
            Returns a JSON array of matching issues with key, summary, status, priority,
            issuetype, and fixVersions fields.""")
    public String jira_search_issues(
            @ToolParam(description = "JQL query string, e.g. 'project=PROJ AND priority=Critical AND status!=Done'")
            String jql,
            @ToolParam(description = "Maximum number of results to return (default: 10, max: 100)")
            Integer maxResults) {
        // Return critical bugs for release 2.4; empty list for all other versions
        if (jql != null && jql.contains("2.4")) {
            return BUGS_2_4;
        }
        return "[]";
    }

    @Tool(description = """
            Retrieves a single Jira issue by its key (e.g. PROJ-123).
            Returns full issue details including status, priority, assignee, and fix versions.""")
    public String jira_get_issue(
            @ToolParam(description = "Jira issue key in format PROJECT-NUMBER, e.g. PROJ-123")
            String issueKey) {
        return switch (issueKey) {
            case "PROJ-101" -> """
                    {"key":"PROJ-101","summary":"NPE in payment processor when card token is null",\
                    "status":"Open","priority":"Critical","issuetype":"Bug",\
                    "assignee":"alice@example.com","fixVersions":["2.4"]}""";
            case "PROJ-108" -> """
                    {"key":"PROJ-108","summary":"Deadlock in session management under high concurrency",\
                    "status":"In Progress","priority":"Critical","issuetype":"Bug",\
                    "assignee":"bob@example.com","fixVersions":["2.4"]}""";
            default -> "{\"error\": \"Issue " + issueKey + " not found\", \"isError\": true}";
        };
    }
}
