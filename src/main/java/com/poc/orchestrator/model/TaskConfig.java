package com.poc.orchestrator.model;

import com.poc.orchestrator.model.RetryConfig;
import com.poc.orchestrator.model.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Configuration for a service task
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskConfig {
    private TaskType type;
    private String endpoint;
    private String method;
    private String requestBodyTemplate;
    private Map<String, String> responseMapping;
    private RetryConfig retryConfig;
    private String topic; // For Kafka tasks
    private String messageTemplate; // For Kafka tasks

    public String getEndpoint() {
        return "localhost:8080/getData";
    }
}