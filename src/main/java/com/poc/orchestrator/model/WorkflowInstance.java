package com.poc.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a running workflow instance
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstance {
    @Id
    private String id;
    private String definitionId;
    private WorkflowStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String currentNodeId;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "var_key")
    @Column(name = "var_value", columnDefinition = "TEXT")
    private Map<String, String> variables = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "completed_task_id")
    @Column(name = "completion_time")
    private Map<String, LocalDateTime> completedTasks = new HashMap<>();

    public static WorkflowInstance create(String definitionId) {
        return WorkflowInstance.builder()
                .id(UUID.randomUUID().toString())
                .definitionId(definitionId)
                .status(WorkflowStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .variables(new HashMap<>())
                .completedTasks(new HashMap<>())
                .build();
    }

    public void complete() {
        this.status = WorkflowStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = WorkflowStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.variables.put("error", errorMessage);
    }
}
