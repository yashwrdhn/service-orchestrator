package com.poc.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a workflow edge connecting source and target nodes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEdge {
    private String id;
    private String sourceNodeId;
    private String targetNodeId;
    private String condition;

}