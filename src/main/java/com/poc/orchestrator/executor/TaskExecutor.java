package com.poc.orchestrator.executor;

import com.poc.orchestrator.model.TaskConfig;
import com.poc.orchestrator.model.TaskResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface TaskExecutor {
    CompletableFuture<TaskResult> execute(TaskConfig config, Map<String, Object> variables);
}