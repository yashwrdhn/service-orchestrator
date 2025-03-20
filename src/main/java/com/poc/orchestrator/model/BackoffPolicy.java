package com.poc.orchestrator.model;


/**
 * Backoff policies for retries
 */
public enum BackoffPolicy {
    FIXED, EXPONENTIAL, LINEAR
}