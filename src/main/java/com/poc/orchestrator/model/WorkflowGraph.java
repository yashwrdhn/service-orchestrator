package com.poc.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a workflow graph with nodes and edges
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowGraph {
    private Map<String, WorkflowNode> nodes = new HashMap<>();
    private Map<String, WorkflowEdge> edges = new HashMap<>();
    private Map<String, List<String>> outgoingEdges = new HashMap<>();
    private Map<String, List<String>> incomingEdges = new HashMap<>();

    public void addNode(WorkflowNode node) {
        nodes.put(node.getId(), node);
        // Initialize edge collections for this node
        if (!outgoingEdges.containsKey(node.getId())) {
            outgoingEdges.put(node.getId(), new ArrayList<>());
        }
        if (!incomingEdges.containsKey(node.getId())) {
            incomingEdges.put(node.getId(), new ArrayList<>());
        }
    }

    public void addEdge(WorkflowEdge edge) {
        edges.put(edge.getId(), edge);

        // Update outgoing edges
        outgoingEdges.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>())
                .add(edge.getTargetNodeId());

        // Update incoming edges
        incomingEdges.computeIfAbsent(edge.getTargetNodeId(), k -> new ArrayList<>())
                .add(edge.getSourceNodeId());
    }

//    public void addEdge(WorkflowEdge edge) {
//        edges.put(edge.getId(), edge);
//
//        // Update outgoing edges for source node
//        List<String> sourceOutgoing = outgoingEdges.getOrDefault(edge.getSourceNodeId(), new ArrayList<>());
//        sourceOutgoing.add(edge.getId());
//        outgoingEdges.put(edge.getSourceNodeId(), sourceOutgoing);
//
//        // Update incoming edges for target node
//        List<String> targetIncoming = incomingEdges.getOrDefault(edge.getTargetNodeId(), new ArrayList<>());
//        targetIncoming.add(edge.getId());
//        incomingEdges.put(edge.getTargetNodeId(), targetIncoming);
//    }

    public List<String> getOutgoingEdgeIds(String nodeId) {
        return outgoingEdges.getOrDefault(nodeId, Collections.emptyList());
    }

    public List<WorkflowEdge> getOutgoingEdges(String nodeId) {
        return getOutgoingEdgeIds(nodeId).stream()
                .map(edgeId -> edges.get(edgeId))
                .collect(Collectors.toList());
    }

    public List<String> getIncomingEdgeIds(String nodeId) {
        return incomingEdges.getOrDefault(nodeId, Collections.emptyList());
    }

    public List<WorkflowNode> getNextNodes(String nodeId) {
        return getOutgoingEdges(nodeId).stream()
                .map(edge -> nodes.get(edge.getTargetNodeId()))
                .collect(Collectors.toList());
    }

    public WorkflowNode getStartNode() {
        return nodes.values().stream()
                .filter(node -> node.getType() == NodeType.START_EVENT)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No start event found in workflow"));
    }

    public void printGraph() {
        System.out.println("Nodes:");
        for (Map.Entry<String, WorkflowNode> entry : nodes.entrySet()) {
            System.out.println("Node ID: " + entry.getKey() + ", Node: " + entry.getValue());
        }

        System.out.println("\nEdges:");
        for (Map.Entry<String, WorkflowEdge> entry : edges.entrySet()) {
            System.out.println("Edge ID: " + entry.getKey() + ", Edge: " + entry.getValue());
        }

        System.out.println("\nOutgoing Edges:");
        for (Map.Entry<String, List<String>> entry : outgoingEdges.entrySet()) {
            System.out.println("Node ID: " + entry.getKey() + ", Outgoing Edges: " + entry.getValue());
        }

        System.out.println("\nIncoming Edges:");
        for (Map.Entry<String, List<String>> entry : incomingEdges.entrySet()) {
            System.out.println("Node ID: " + entry.getKey() + ", Incoming Edges: " + entry.getValue());
        }
    }
}

