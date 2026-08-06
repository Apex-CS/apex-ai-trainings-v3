package com.owasp.aiassistant.config;

import org.mlflow.tracking.MlflowClient;
import org.mlflow.tracking.creds.DatabricksMlflowHostCreds;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MlflowProperties.class)
public class MlflowConfig {

    @Bean
    @ConditionalOnProperty(name = "app.mlflow.enabled", havingValue = "true", matchIfMissing = true)
    MlflowClient mlflowClient(MlflowProperties properties, DatabricksProperties databricksProperties) {
        if (properties.isDatabricks()) {
            return databricksMlflowClient(databricksProperties);
        }
        return new MlflowClient(properties.getTrackingUri());
    }

    private static MlflowClient databricksMlflowClient(DatabricksProperties databricksProperties) {
        String host = databricksProperties.getHost();
        String token = databricksProperties.getToken();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException(
                    "app.databricks.host (DATABRICKS_HOST) is required when MLFLOW_TRACKING_URI=databricks");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "app.databricks.token (DATABRICKS_TOKEN) is required when MLFLOW_TRACKING_URI=databricks");
        }
        return new MlflowClient(new DatabricksMlflowHostCreds(host, token));
    }
}
