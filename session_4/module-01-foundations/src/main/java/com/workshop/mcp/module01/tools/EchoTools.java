package com.workshop.mcp.module01.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Echo and System Info MCP Tools — Module 01 Foundations.
 *
 * <p>Demonstrates tools that return structured JSON strings (which LLMs can parse)
 * and stateless utility tools. Also shows that tool methods can have zero parameters.
 */
@Service
public class EchoTools {

    @Tool(description = "Echoes the input message back to the caller unchanged")
    public String echo(@ToolParam(description = "The message to echo back") String message) {
        return "Echo: " + message;
    }

    @Tool(description = "Returns the current server timestamp in ISO-8601 UTC format")
    public String currentTimestamp() {
        return Instant.now().toString();
    }

    /**
     * Returns JVM and OS runtime information as a JSON string.
     *
     * <p>Demonstrates that tools can return complex structured data as a JSON string.
     * The LLM receives this as plain text content and can extract relevant fields.
     * NOTE: Do NOT use System.out.println() — it corrupts the stdio JSON-RPC stream.
     */
    @Tool(description = "Returns JVM version, operating system, CPU count, and max heap memory as a JSON object")
    public String systemInfo() {
        return """
                {
                  "javaVersion": "%s",
                  "javaVendor": "%s",
                  "osName": "%s",
                  "osArch": "%s",
                  "availableProcessors": %d,
                  "maxHeapMemoryMb": %d,
                  "usedHeapMemoryMb": %d,
                  "threadModel": "%s"
                }""".formatted(
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().maxMemory() / (1024 * 1024),
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024),
                Thread.currentThread().isVirtual() ? "Virtual Thread (Project Loom)" : "Platform Thread");
    }
}
