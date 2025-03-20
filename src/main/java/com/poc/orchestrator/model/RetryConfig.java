package com.poc.orchestrator.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Retry configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryConfig {
    private int maxAttempts;
    private BackoffPolicy backoffPolicy;
    private long initialDelay;
    private long maxDelay;
}
