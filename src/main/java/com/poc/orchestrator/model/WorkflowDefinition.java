package com.poc.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import java.time.LocalDateTime;

/**
 * Represents a workflow definition parsed from BPMN
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition {
    @Id
    private String id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String bpmnXml;

}
