package com.poc.orchestrator.controller;


import com.poc.orchestrator.engine.WorkflowEngine;
import com.poc.orchestrator.model.WorkflowDefinition;
import com.poc.orchestrator.repository.WorkflowDefinitionRepository;
import com.poc.orchestrator.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {
    private WorkflowEngine workflowEngine;
    private WorkflowDefinitionRepository definitionRepository;
    private WorkflowInstanceRepository instanceRepository;

    /*
     * Workflow Definition Endpoints
     */

    @PostMapping("/definitions")
    public ResponseEntity<WorkflowDefinition> createWorkflowDefinition(@RequestBody WorkflowDefinition definition) {
        WorkflowDefinition definition = WorkflowDefinition.builder()
                .id(UUID.randomUUID().toString())
                .name(definitionDTO.getName())
                .description(definitionDTO.getDescription())
                .bpmnXml(definitionDTO.getBpmnXml())
                .createdAt(LocalDateTime.now())
                .build();

        definition = definitionRepository.save(definition);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(convertToDefinitionDTO(definition));
    }

    @GetMapping("/definitions")
    public ResponseEntity<List<WorkflowDefinitionDTO>> getAllWorkflowDefinitions() {
        List<WorkflowDefinition> definitions = definitionRepository.findAll();
        List<WorkflowDefinitionDTO> dtos = definitions.stream()
                .map(this::convertToDefinitionDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/definitions/{id}")
    public ResponseEntity<WorkflowDefinitionDTO> getWorkflowDefinition(@PathVariable String id) {
        WorkflowDefinition definition = definitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found"));

        return ResponseEntity.ok(convertToDefinitionDTO(definition));
    }

    @PutMapping("/definitions/{id}")
    public ResponseEntity<WorkflowDefinitionDTO> updateWorkflowDefinition(
            @PathVariable String id,
            @RequestBody WorkflowDefinitionDTO definitionDTO) {

        WorkflowDefinition definition = definitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found"));

        definition.setName(definitionDTO.getName());
        definition.setDescription(definitionDTO.getDescription());
        definition.setBpmnXml(definitionDTO.getBpmnXml());

        definition = definitionRepository.save(definition);

        return ResponseEntity.ok(convertToDefinitionDTO(definition));
    }

    @DeleteMapping("/definitions/{id}")
    public ResponseEntity<Void> deleteWorkflowDefinition(@PathVariable String id) {
        if (!definitionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found");
        }

        definitionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /*
     * Workflow Instance Endpoints
     */

    @PostMapping("/instances")
    public ResponseEntity<WorkflowInstanceDTO> startWorkflowInstance(@RequestBody WorkflowStartRequest request) {
        if (!definitionRepository.existsById(request.getDefinitionId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found");
        }

        WorkflowInstance instance = workflowEngine.startWorkflow(
                request.getDefinitionId(),
                request.getVariables());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(convertToInstanceDTO(instance));
    }

    @GetMapping("/instances")
    public ResponseEntity<List<WorkflowInstanceDTO>> getAllWorkflowInstances() {
        List<WorkflowInstance> instances = instanceRepository.findAll();
        List<WorkflowInstanceDTO> dtos = instances.stream()
                .map(this::convertToInstanceDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/instances/{id}")
    public ResponseEntity<WorkflowInstanceDTO> getWorkflowInstance(@PathVariable String id) {
        WorkflowInstance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));

        return ResponseEntity.ok(convertToInstanceDTO(instance));
    }

    @GetMapping("/definitions/{definitionId}/instances")
    public ResponseEntity<List<WorkflowInstanceDTO>> getWorkflowInstancesByDefinition(@PathVariable String definitionId) {
        List<WorkflowInstance> instances = instanceRepository.findByDefinitionId(definitionId);
        List<WorkflowInstanceDTO> dtos = instances.stream()
                .map(this::convertToInstanceDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/instances/{id}/status")
    public ResponseEntity<WorkflowStatusResponse> getWorkflowInstanceStatus(@PathVariable String id) {
        WorkflowInstance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));

        WorkflowStatusResponse status = WorkflowStatusResponse.builder()
                .instanceId(instance.getId())
                .status(instance.getStatus())
                .currentNodeId(instance.getCurrentNodeId())
                .startTime(instance.getStartTime())
                .endTime(instance.getEndTime())
                .completedTasks(instance.getCompletedTasks())
                .build();

        return ResponseEntity.ok(status);
    }

    @PostMapping("/instances/{id}/suspend")
    public ResponseEntity<WorkflowInstanceDTO> suspendWorkflowInstance(@PathVariable String id) {
        WorkflowInstance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));

        if (instance.getStatus() != WorkflowStatus.RUNNING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot suspend workflow instance. Current status: " + instance.getStatus());
        }

        instance.setStatus(WorkflowStatus.SUSPENDED);
        instance = instanceRepository.save(instance);

        return ResponseEntity.ok(convertToInstanceDTO(instance));
    }

    @PostMapping("/instances/{id}/resume")
    public ResponseEntity<WorkflowInstanceDTO> resumeWorkflowInstance(@PathVariable String id) {
        WorkflowInstance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));

        if (instance.getStatus() != WorkflowStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot resume workflow instance. Current status: " + instance.getStatus());
        }

        instance.setStatus(WorkflowStatus.RUNNING);
        instance = instanceRepository.save(instance);

        workflowEngine.executeWorkflow(instance);

        return ResponseEntity.ok(convertToInstanceDTO(instance));
    }

    /*
     * Helper methods
     */

    private WorkflowDefinitionDTO convertToDefinitionDTO(WorkflowDefinition definition) {
        return WorkflowDefinitionDTO.builder()
                .id(definition.getId())
                .name(definition.getName())
                .description(definition.getDescription())
                .bpmnXml(definition.getBpmnXml())
                .createdAt(definition.getCreatedAt())
                .build();
    }

    private WorkflowInstanceDTO convertToInstanceDTO(WorkflowInstance instance) {
        return WorkflowInstanceDTO.builder()
                .id(instance.getId())
                .definitionId(instance.getDefinitionId())
                .status(instance.getStatus())
                .currentNodeId(instance.getCurrentNodeId())
                .startTime(instance.getStartTime())
                .endTime(instance.getEndTime())
                .variables(instance.getVariables())
                .completedTasks(instance.getCompletedTasks())
                .build();
    }
}