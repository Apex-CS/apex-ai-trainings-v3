package com.workshop.mcp.module02;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.workshop.mcp.module02.client.JiraMcpClientService;
import com.workshop.mcp.module02.dto.JiraIssueDTO;

@Component
public class JiraDemoRunner implements CommandLineRunner {

    private final JiraMcpClientService service;

    public JiraDemoRunner(JiraMcpClientService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n=== Jira MCP Client Demo ===");

        var tools = service.listAvailableTools();
        System.out.println("Available tools: "
                + tools.stream().map(t -> t.name()).toList());

        System.out.println("\nFetching issue PROJ-101...");
        JiraIssueDTO issue = service.getIssue("PROJ-101");
        System.out.printf("Issue: %s - %s [%s %s, status=%s]%n",
                issue.key(), issue.summary(),
                issue.priority().toUpperCase(), issue.issueType().toUpperCase(),
                issue.status());

        System.out.println("\nSearching critical bugs for release 2.4...");
        List<JiraIssueDTO> blockers = service.searchCriticalBugs("PROJ", "2.4");
        System.out.printf("Found %d critical open bugs:%n", blockers.size());
        blockers.forEach(b -> System.out.printf("  - %s: %s%n", b.key(), b.summary()));

        boolean blocked = blockers.stream().anyMatch(JiraIssueDTO::isReleaseBlocker);
        System.out.println(blocked
                ? "\nRelease 2.4 is BLOCKED \u2014 resolve critical bugs before deploying."
                : "\nRelease 2.4 is CLEAR \u2014 no critical blockers found.");

        System.out.println("\n=== Demo Complete ===");
    }
}
