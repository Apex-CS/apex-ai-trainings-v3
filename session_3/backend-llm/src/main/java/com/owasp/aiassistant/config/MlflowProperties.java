package com.owasp.aiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mlflow")
public class MlflowProperties {

    private boolean enabled = true;
    private String trackingUri = "http://localhost:5000";
    private String experimentName = "owasp-chat";
    private boolean autoCreateExperiment = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTrackingUri() {
        return trackingUri;
    }

    public void setTrackingUri(String trackingUri) {
        this.trackingUri = trackingUri;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public boolean isAutoCreateExperiment() {
        return autoCreateExperiment;
    }

    public void setAutoCreateExperiment(boolean autoCreateExperiment) {
        this.autoCreateExperiment = autoCreateExperiment;
    }

    public boolean isDatabricks() {
        return trackingUri != null && "databricks".equalsIgnoreCase(trackingUri.trim());
    }

    /**
     * REST API base URL for trace endpoints. For Databricks, this is the workspace host;
     * for local MLflow, it is the tracking server URL.
     */
    public String resolveApiBaseUrl(String databricksHost) {
        if (isDatabricks()) {
            return normalizeBaseUrl(requireDatabricksHost(databricksHost));
        }
        return normalizeBaseUrl(trackingUri);
    }

    private static String requireDatabricksHost(String databricksHost) {
        if (databricksHost == null || databricksHost.isBlank()) {
            throw new IllegalStateException(
                    "app.databricks.host (DATABRICKS_HOST) is required when MLFLOW_TRACKING_URI=databricks");
        }
        return databricksHost;
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5000";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
