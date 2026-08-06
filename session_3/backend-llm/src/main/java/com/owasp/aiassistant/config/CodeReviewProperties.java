package com.owasp.aiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.code-review")
public class CodeReviewProperties {

    private long maxUploadBytes = 10L * 1024 * 1024;
    private long maxUncompressedBytes = 5L * 1024 * 1024;
    private int maxFiles = 200;
    private int maxTextChars = 500_000;

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public void setMaxUploadBytes(long maxUploadBytes) {
        this.maxUploadBytes = maxUploadBytes;
    }

    public long getMaxUncompressedBytes() {
        return maxUncompressedBytes;
    }

    public void setMaxUncompressedBytes(long maxUncompressedBytes) {
        this.maxUncompressedBytes = maxUncompressedBytes;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public int getMaxTextChars() {
        return maxTextChars;
    }

    public void setMaxTextChars(int maxTextChars) {
        this.maxTextChars = maxTextChars;
    }
}
