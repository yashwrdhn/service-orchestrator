package com.poc.orchestrator.controller;


import com.poc.orchestrator.engine.WorkflowEngine;
import com.poc.orchestrator.model.WorkflowDefinition;
import com.poc.orchestrator.model.WorkflowGraph;
import com.poc.orchestrator.model.WorkflowInstance;
import com.poc.orchestrator.model.WorkflowStatus;
import com.poc.orchestrator.repository.WorkflowDefinitionRepository;
import com.poc.orchestrator.repository.WorkflowGraphRepository;
import com.poc.orchestrator.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@Component
public class WorkflowController {
    private WorkflowEngine workflowEngine;

    @Autowired
     WorkflowDefinitionRepository definitionRepository;
    private WorkflowInstanceRepository instanceRepository;

    @Autowired
    private WorkflowGraphRepository graphRepository;
    /*
     * Workflow Definition Endpoints
     */
    public String readXmlAsString(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            byte[] bytes = Files.readAllBytes(Paths.get(resource.getURI()));
            return new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/definitions")
    public ResponseEntity<WorkflowDefinition> createWorkflowDefinition(@RequestBody WorkflowDefinition definitionDTO) {


        WorkflowDefinition definition = WorkflowDefinition.builder()
                .definitionId(UUID.randomUUID().toString())
                .name(definitionDTO.getName())
                .description(definitionDTO.getDescription())
                .bpmnXml(readXmlAsString("diagram.bpmn"))
                .createdAt(LocalDateTime.now())
                .build();

        definition = definitionRepository.save(definition);

        WorkflowGraph graph = graphRepository.getGraph(definition.getDefinitionId());
        graph.printGraph();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(definition);
    }

//    @GetMapping("/definitions")
//    public ResponseEntity<List<WorkflowDefinition>> getAllWorkflowDefinitions() {
//        List<WorkflowDefinition> definitions = definitionRepository.findAll();
//        List<WorkflowDefinition> dtos = definitions.stream()
//                .map(this::convertToDefinitionDTO)
//                .collect(Collectors.toList());
//
//        return ResponseEntity.ok(dtos);
//    }
//
//    @GetMapping("/definitions/{id}")
//    public ResponseEntity<WorkflowDefinition> getWorkflowDefinition(@PathVariable String id) {
//        WorkflowDefinition definition = definitionRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found"));
//
//        return ResponseEntity.ok(convertToDefinitionDTO(definition));
//    }
//
//    @PutMapping("/definitions/{id}")
//    public ResponseEntity<WorkflowDefinition> updateWorkflowDefinition(
//            @PathVariable String id,
//            @RequestBody WorkflowDefinition definitionDTO) {
//
//        WorkflowDefinition definition = definitionRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found"));
//
//        definition.setName(definitionDTO.getName());
//        definition.setDescription(definitionDTO.getDescription());
//        definition.setBpmnXml(definitionDTO.getBpmnXml());
//
//        definition = definitionRepository.save(definition);
//
//        return ResponseEntity.ok(convertToDefinitionDTO(definition));
//    }
//
//    @DeleteMapping("/definitions/{id}")
//    public ResponseEntity<Void> deleteWorkflowDefinition(@PathVariable String id) {
//        if (!definitionRepository.existsById(id)) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found");
//        }
//
//        definitionRepository.deleteById(id);
//        return ResponseEntity.noContent().build();
//    }
//
//    /*
//     * Workflow Instance Endpoints
//     */
//
    @PostMapping("/instances")
    public ResponseEntity<WorkflowInstance> startWorkflowInstance(@RequestBody Map<String, Object> request) {
        if (!definitionRepository.existsById((String) request.get("definitionId"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found");
        }

        WorkflowInstance instance = workflowEngine.startWorkflow(
                (String) request.get("definitionId"),
                (Map<String, Object>) request.get("variables"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(instance);
    }

//    @GetMapping("/instances")
//    public ResponseEntity<List<WorkflowDefinition>> getAllWorkflowInstances() {
//        List<WorkflowInstance> instances = instanceRepository.findAll();
//        List<WorkflowDefinition> dtos = instances.stream()
//                .map(this::convertToInstanceDTO)
//                .collect(Collectors.toList());
//
//        return ResponseEntity.ok(dtos);
//    }
//
//    @GetMapping("/instances/{id}")
//    public ResponseEntity<WorkflowDefinition> getWorkflowInstance(@PathVariable String id) {
//        WorkflowInstance instance = instanceRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));
//
//        return ResponseEntity.ok(convertToInstanceDTO(instance));
//    }
//
//    @GetMapping("/definitions/{definitionId}/instances")
//    public ResponseEntity<List<WorkflowDefinition>> getWorkflowInstancesByDefinition(@PathVariable String definitionId) {
//        List<WorkflowInstance> instances = instanceRepository.findByDefinitionId(definitionId);
//        List<WorkflowDefinition> dtos = instances.stream()
//                .map(this::convertToInstanceDTO)
//                .collect(Collectors.toList());
//
//        return ResponseEntity.ok(dtos);
//    }
//
//    @GetMapping("/instances/{id}/status")
//    public ResponseEntity<String> getWorkflowInstanceStatus(@PathVariable String id) {
//        WorkflowInstance instance = instanceRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));
//
////        WorkflowStatusResponse status = WorkflowStatusResponse.builder()
////                .instanceId(instance.getId())
////                .status(instance.getStatus())
////                .currentNodeId(instance.getCurrentNodeId())
////                .startTime(instance.getStartTime())
////                .endTime(instance.getEndTime())
////                .completedTasks(instance.getCompletedTasks())
////                .build();
//
//        return ResponseEntity.ok(instance.toString());
//    }
//
//    @PostMapping("/instances/{id}/suspend")
//    public ResponseEntity<WorkflowInstance> suspendWorkflowInstance(@PathVariable String id) {
//        WorkflowInstance instance = instanceRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));
//
//        if (instance.getStatus() != WorkflowStatus.RUNNING) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                    "Cannot suspend workflow instance. Current status: " + instance.getStatus());
//        }
//
//        instance.setStatus(WorkflowStatus.SUSPENDED);
//        instance = instanceRepository.save(instance);
//
//        return ResponseEntity.ok(convertToInstanceDTO(instance));
//    }
//
//    @PostMapping("/instances/{id}/resume")
//    public ResponseEntity<WorkflowInstance> resumeWorkflowInstance(@PathVariable String id) {
//        WorkflowInstance instance = instanceRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));
//
//        if (instance.getStatus() != WorkflowStatus.SUSPENDED) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                    "Cannot resume workflow instance. Current status: " + instance.getStatus());
//        }
//
//        instance.setStatus(WorkflowStatus.RUNNING);
//        instance = instanceRepository.save(instance);
//
//        workflowEngine.executeWorkflow(instance);
//
//        return ResponseEntity.ok(convertToInstanceDTO(instance));
//    }
//
//    /*
//     * Helper methods
//     */
//
//    private WorkflowDefinition convertToDefinitionDTO(WorkflowDefinition definition) {
//        return WorkflowDefinition.builder()
//                .id(definition.getId())
//                .name(definition.getName())
//                .description(definition.getDescription())
//                .bpmnXml(definition.getBpmnXml())
//                .createdAt(definition.getCreatedAt())
//                .build();
//    }
//
//    private WorkflowInstance convertToInstanceDTO(WorkflowInstance instance) {
//        return WorkflowInstance.builder()
//                .id(UUID.randomUUID().toString())
//                .definitionId(instance.getDefinitionId())
//                .status(WorkflowStatus.RUNNING)
//                .startTime(LocalDateTime.now())
//                .variables(new HashMap<>())
//                .completedTasks(new HashMap<>())
//                .build();
//    }
}