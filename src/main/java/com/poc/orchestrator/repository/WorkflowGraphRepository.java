package com.poc.orchestrator.repository;

import com.poc.orchestrator.BpmnParser;
import com.poc.orchestrator.model.WorkflowDefinition;
import com.poc.orchestrator.model.WorkflowGraph;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WorkflowGraphRepository {
    private final Map<String, WorkflowGraph> graphCache = new ConcurrentHashMap<>();
    @Autowired
    BpmnParser bpmnParser;
    @Autowired
    WorkflowDefinitionRepository definitionRepository;

    /**
     * Get workflow graph by definition ID
     */
    public WorkflowGraph getGraph(String definitionId) {
        return graphCache.computeIfAbsent(definitionId, id -> {
            WorkflowDefinition definition = definitionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + id));
            return bpmnParser.parse(definition.getBpmnXml());
        });
    }

    /**
     * Store or update a workflow graph
     */
    public void storeGraph(String definitionId, WorkflowGraph graph) {
        graphCache.put(definitionId, graph);
    }

    /**
     * Remove workflow graph from cache
     */
    public void removeGraph(String definitionId) {
        graphCache.remove(definitionId);
    }
}