package com.workshop.mcp.module02.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO mapping the {@code systemInfo} tool response from Module 01's MCP server.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} is mandatory — the server may
 * add fields in the future and we want the client to remain forward-compatible.
 *
 * <p>Example JSON returned by the tool:
 * <pre>{@code
 * {
 *   "javaVersion":          "21.0.x",
 *   "javaVendor":           "Eclipse Adoptium",
 *   "osName":               "Linux",
 *   "osArch":               "amd64",
 *   "availableProcessors":  4,
 *   "maxHeapMemoryMb":      256,
 *   "usedHeapMemoryMb":     45,
 *   "threadModel":          "Virtual Thread (Project Loom)"
 * }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SystemInfoDTO(

        @JsonProperty("javaVersion")
        String javaVersion,

        @JsonProperty("javaVendor")
        String javaVendor,

        @JsonProperty("osName")
        String osName,

        @JsonProperty("osArch")
        String osArch,

        @JsonProperty("availableProcessors")
        int availableProcessors,

        @JsonProperty("maxHeapMemoryMb")
        long maxHeapMemoryMb,

        @JsonProperty("usedHeapMemoryMb")
        long usedHeapMemoryMb,

        @JsonProperty("threadModel")
        String threadModel

) {
    /** Returns true if the server is running on Java Virtual Threads (Project Loom). */
    public boolean isVirtualThread() {
        return threadModel != null && threadModel.contains("Virtual");
    }
}
