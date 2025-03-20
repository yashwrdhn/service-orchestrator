package com.poc.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a node in the workflow graph
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNode {
    private String id;
    private String name;
    private NodeType type;
    private Map<String, Object> properties = new HashMap<>();
    private TaskConfig taskConfig;
}