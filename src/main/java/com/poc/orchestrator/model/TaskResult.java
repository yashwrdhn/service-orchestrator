package com.poc.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResult {
    private TaskStatus status;
    private Map<String, Object> outputVariables;

    public static TaskResult success(Map<String, Object> outputVariables) {
        return TaskResult.builder()
                .status(TaskStatus.SUCCESS)
                .outputVariables(outputVariables)
                .build();
    }

    public static TaskResult failure(String errorMessage) {
        Map<String, Object> errorOutput = new HashMap<>();
        errorOutput.put("error", errorMessage);
        return TaskResult.builder()
                .status(TaskStatus.FAILURE)
                .outputVariables(errorOutput)
                .build();
    }
}