package com.workshop.mcp.module02;

import com.workshop.mcp.module02.client.JiraMcpClientService;
import com.workshop.mcp.module02.dto.SystemInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Demo runner that exercises all four MCP client patterns on startup.
 * Connects to Module 01's server via stdio transport.
 */
@Component
public class JiraDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(JiraDemoRunner.class);

    private final JiraMcpClientService service;

    public JiraDemoRunner(JiraMcpClientService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n=== MCP Client Demo (Module 02) ===");
        System.out.println("Transport: stdio  |  Server: Module 01 (workshop-hello-mcp)\n");

        // Pattern 1: Dynamic tool discovery
        var tools = service.listAvailableTools();
        System.out.println("[Pattern 1] Available tools: "
                + tools.stream().map(t -> t.name()).toList());

        // Pattern 2: Tool invocation with typed numeric response
        double sum = service.add(7, 3);
        System.out.printf("%n[Pattern 2] add(7, 3) = %.1f%n", sum);

        double sum2 = service.add(100, 200);
        System.out.printf("[Pattern 2] add(100, 200) = %.1f%n", sum2);

        // Pattern 3: Structured JSON response deserialized to DTO
        System.out.println("\n[Pattern 3] Calling systemInfo() — JSON → DTO...");
        SystemInfoDTO info = service.getSystemInfo();
        System.out.printf("  Java     : %s (%s)%n", info.javaVersion(), info.javaVendor());
        System.out.printf("  OS       : %s (%s)%n", info.osName(), info.osArch());
        System.out.printf("  CPUs     : %d%n", info.availableProcessors());
        System.out.printf("  Heap     : %d MB used / %d MB max%n",
                info.usedHeapMemoryMb(), info.maxHeapMemoryMb());
        System.out.printf("  Threads  : %s%n", info.threadModel());
        System.out.printf("  Virtual? : %b%n", info.isVirtualThread());

        // Pattern 4: Error handling — isError:true in MCP response
        System.out.println("\n[Pattern 4] Calling divide(10, 0) — expect isError:true...");
        try {
            service.divide(10, 0);
            System.out.println("  (no error — unexpected)");
        } catch (JiraMcpClientService.JiraMcpException e) {
            System.out.println("  Caught JiraMcpException: " + e.getMessage());
            System.out.println("  \u2713 Server returned isError:true — client surfaced it as Java exception");
        }

        System.out.println("\n=== Demo Complete ===\n");
    }
}
