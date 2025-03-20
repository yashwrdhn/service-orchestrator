package com.poc.orchestrator.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.orchestrator.Service.WorkflowTaskScheduler;
//import com.poc.orchestrator.executor.KafkaTaskExecutor;
import com.poc.orchestrator.executor.TaskExecutor;
import com.poc.orchestrator.executor.TaskExecutorFactory;
import com.poc.orchestrator.model.*;
import com.poc.orchestrator.repository.WorkflowGraphRepository;
import com.poc.orchestrator.repository.WorkflowInstanceRepository;
import com.poc.orchestrator.util.ExpressionEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.poc.orchestrator.model.NodeType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngine {
    private WorkflowGraphRepository graphRepository;
    private WorkflowInstanceRepository instanceRepository;
    private TaskExecutorFactory taskExecutorFactory;
    private WorkflowTaskScheduler taskScheduler;
    private ExpressionEvaluator expressionEvaluator;
    private ExpressionParser expressionParser = new SpelExpressionParser();
    private ObjectMapper objectMapper;

    // Track currently executing tasks
    private final Map<String, CompletableFuture<TaskResult>> executingTasks = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    /**
     * Start a new workflow instance
     */
    @Transactional
    public WorkflowInstance startWorkflow(String definitionId, Map<String, Object> initialVariables) {
        // Create a new workflow instance
        WorkflowInstance instance = WorkflowInstance.create(definitionId);

        // Store initial variables as JSON strings
        if (initialVariables != null) {
            for (Map.Entry<String, Object> entry : initialVariables.entrySet()) {
                try {
                    String value = (entry.getValue() instanceof String)
                            ? (String) entry.getValue()
                            : objectMapper.writeValueAsString(entry.getValue());
                    instance.getVariables().put(entry.getKey(), value);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize variable {}: {}", entry.getKey(), e.getMessage());
                    instance.getVariables().put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        }

        // Save the instance
        instance = instanceRepository.save(instance);

        // Start execution from the start event
        executeWorkflow(instance);

        return instance;
    }

    /**
     * Execute or continue workflow instance
     */
    @Transactional
    public void executeWorkflow(WorkflowInstance instance) {
        if (instance.getStatus() != WorkflowStatus.RUNNING) {
            log.info("Workflow instance {} is not in RUNNING state, skipping execution", instance.getId());
            return;
        }

        WorkflowGraph graph = graphRepository.getGraph(instance.getDefinitionId());

        // If currentNodeId is null, start from the start event
        if (instance.getCurrentNodeId() == null) {
            WorkflowNode startNode = graph.getStartNode();
            instance.setCurrentNodeId(startNode.getId());
            instanceRepository.save(instance);

            // Execute nodes after the start event
            executeNextNodes(instance, startNode.getId());
        }
    }

    /**
     * Execute nodes that follow a completed node
     */
    @Transactional
    public void executeNextNodes(WorkflowInstance instance, String completedNodeId) {
        WorkflowGraph graph = graphRepository.getGraph(instance.getDefinitionId());

        // Find outgoing edges from the completed node
        List<WorkflowEdge> outgoingEdges = graph.getOutgoingEdges(completedNodeId);

        // Convert variables to object map for expression evaluation
        Map<String, Object> variables = deserializeVariables(instance.getVariables());

        // Check conditions and find next nodes to execute
        List<WorkflowNode> nodesToExecute = new ArrayList<>();

        for (WorkflowEdge edge : outgoingEdges) {
            // If edge has a condition, evaluate it
            if (edge.getCondition() != null && !edge.getCondition().isEmpty()) {
                //TODO in evaluate check if all incomingNodes are completed
                boolean conditionMet = evaluateCondition(edge.getCondition(), variables);
                if (!conditionMet) {
                    continue;
                }
            }
            //TODO have to insert targetNode to centeralised Queue
            // Get target node
            WorkflowNode targetNode = graph.getNodes().get(edge.getTargetNodeId());
            if (targetNode != null) {
                nodesToExecute.add(targetNode);
            }
        }

        // Execute each next node
        //TODO this will be removed then
        for (WorkflowNode node : nodesToExecute) {
            executeNode(instance, node, variables);
        }

        // If no next nodes, check if we've reached the end
        if (nodesToExecute.isEmpty()) {
            // Check if all tasks are completed
            boolean allNodesCompleted = true;
            for (WorkflowNode node : graph.getNodes().values()) {
                if (node.getType() != NodeType.START_EVENT && node.getType() != END_EVENT) {
                    if (!instance.getCompletedTasks().containsKey(node.getId())) {
                        allNodesCompleted = false;
                        break;
                    }
                }
            }

            if (allNodesCompleted) {
                completeWorkflow(instance);
            }
        }
    }

    /**
     * Execute a specific node
     */
    private void executeNode(WorkflowInstance instance, WorkflowNode node, Map<String, Object> variables) {
        // Update current node in instance

        instance.setCurrentNodeId(node.getId());
        instanceRepository.save(instance);
        log.info("Executing node: {} (type: {}) in workflow instance: {}",
                node.getId(), node.getType(), instance.getId());

        switch (node.getType()) {
            case SERVICE_TASK:
                executeServiceTask(instance, node, variables);
                break;
            case END_EVENT:
                markNodeAsCompleted(instance, node.getId());
                completeWorkflow(instance);
                break;
            case PARALLEL_GATEWAY:
                handleParallelGateway(instance, node, variables);
                break;
            default:
                log.warn("Unsupported node type: {}", node.getType());
                markNodeAsCompleted(instance, node.getId());
                executeNextNodes(instance, node.getId());
        }
    }

    /**
     * Handle parallel gateway node
     */
    private void handleParallelGateway(WorkflowInstance instance, WorkflowNode node, Map<String, Object> variables) {
        WorkflowGraph graph = graphRepository.getGraph(instance.getDefinitionId());

        if (isJoiningGateway(graph, node)) {
            // This is a joining gateway, check if all incoming branches are completed
            List<WorkflowEdge> incomingEdges = graph.getIncomingEdgeIds(node.getId()).stream()
                    .map(id -> graph.getEdges().get(id))
                    .collect(Collectors.toList());

            boolean allIncomingCompleted = true;
            for (WorkflowEdge edge : incomingEdges) {
                if (!instance.getCompletedTasks().containsKey(edge.getSourceNodeId())) {
                    allIncomingCompleted = false;
                    break;
                }
            }

            if (allIncomingCompleted) {
                // All incoming branches are complete, mark gateway as complete and continue
                markNodeAsCompleted(instance, node.getId());
                executeNextNodes(instance, node.getId());
            }
        } else {
            // This is a forking gateway, just mark it as complete and continue to all outgoing paths
            markNodeAsCompleted(instance, node.getId());
            executeNextNodes(instance, node.getId());
        }
    }

    /**
     * Check if a gateway is a joining gateway (has multiple incoming edges)
     */
    private boolean isJoiningGateway(WorkflowGraph graph, WorkflowNode node) {
        return graph.getIncomingEdgeIds(node.getId()).size() > 1;
    }

    /**
     * Execute a service task
     */
    private void executeServiceTask(WorkflowInstance instance, WorkflowNode node, Map<String, Object> variables) {
        TaskConfig taskConfig = node.getTaskConfig();
        if (taskConfig == null) {
            log.error("No task configuration found for node: {}", node.getId());
            failWorkflow(instance, "Missing task configuration for node: " + node.getId());
            return;
        }

        // Get task executor
        TaskExecutor executor = taskExecutorFactory.getExecutor(taskConfig.getType());

        // Execute task
        String taskExecutionId = instance.getId() + ":" + node.getId();
        CompletableFuture<TaskResult> future = executor.execute(taskConfig, variables);
        executingTasks.put(taskExecutionId, future);

        future.thenAccept(result -> {
            executingTasks.remove(taskExecutionId);

            if (result.getStatus() == TaskStatus.SUCCESS) {
                handleTaskSuccess(instance, node, result);
            } else if (result.getStatus() == TaskStatus.RETRY) {
                handleTaskRetry(instance, node, variables, taskConfig.getRetryConfig());
            } else {
                handleTaskFailure(instance, node, result);
            }
        }).exceptionally(ex -> {
            executingTasks.remove(taskExecutionId);
            log.error("Task execution failed with exception: {}", node.getId(), ex);

            failWorkflow(instance, "Task execution failed with exception: " + ex.getMessage());
            return null;
        });
    }

    /**
     * Handle successful task execution
     */
    @Transactional
    public void handleTaskSuccess(WorkflowInstance instance, WorkflowNode node, TaskResult result) {
        // Update variables with task output
        updateInstanceVariables(instance, result.getOutputVariables());

        // Mark node as completed
        markNodeAsCompleted(instance, node.getId());

        // Continue workflow execution
        executeNextNodes(instance, node.getId());
    }

    /**
     * Handle task retry
     */
    private void handleTaskRetry(WorkflowInstance instance, WorkflowNode node,
                                 Map<String, Object> variables, RetryConfig retryConfig) {
        if (retryConfig == null) {
            log.warn("No retry configuration for node: {}, failing task", node.getId());
            failWorkflow(instance, "Task execution failed and no retry configuration available");
            return;
        }

        // Schedule retry
        String taskId = instance.getId() + ":" + node.getId() + ":retry";
        taskScheduler.scheduleWithRetry(taskId, () -> {
            try {
                // Re-execute the node with updated variables
                Map<String, Object> currentVars = deserializeVariables(
                        instanceRepository.findById(instance.getId())
                                .orElseThrow().getVariables());

                executeNode(instance, node, currentVars);
                return true;
            } catch (Exception e) {
                log.error("Retry execution failed for node: {}", node.getId(), e);
                return false;
            }
        }, retryConfig, 0);
    }

    /**
     * Handle task failure
     */
    @Transactional
    public void handleTaskFailure(WorkflowInstance instance, WorkflowNode node, TaskResult result) {
        // Check if we should retry
        if (node.getTaskConfig() != null && node.getTaskConfig().getRetryConfig() != null) {
            Map<String, Object> vars = deserializeVariables(instance.getVariables());
            handleTaskRetry(instance, node, vars, node.getTaskConfig().getRetryConfig());
        } else {
            // No retry, fail the workflow
            if (result.getOutputVariables() != null && result.getOutputVariables().containsKey("error")) {
                failWorkflow(instance, String.valueOf(result.getOutputVariables().get("error")));
            } else {
                failWorkflow(instance, "Task execution failed for node: " + node.getId());
            }
        }
    }

    /**
     * Mark a node as completed
     */
    @Transactional
    public void markNodeAsCompleted(WorkflowInstance instance, String nodeId) {
        instance.getCompletedTasks().put(nodeId, LocalDateTime.now());
        instanceRepository.save(instance);
        log.info("Node {} completed in workflow instance {}", nodeId, instance.getId());
    }

    /**
     * Complete the workflow instance
     */
    @Transactional
    public void completeWorkflow(WorkflowInstance instance) {
        instance.complete();
        instanceRepository.save(instance);
        log.info("Workflow instance {} completed successfully", instance.getId());
    }

    /**
     * Fail the workflow instance
     */
    @Transactional
    public void failWorkflow(WorkflowInstance instance, String errorMessage) {
        instance.fail(errorMessage);
        instanceRepository.save(instance);
        log.error("Workflow instance {} failed: {}", instance.getId(), errorMessage);

        // Cancel any executing tasks
        for (String taskId : new ArrayList<>(executingTasks.keySet())) {
            if (taskId.startsWith(instance.getId() + ":")) {
                CompletableFuture<TaskResult> future = executingTasks.remove(taskId);
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                }
            }
        }
    }

    /**
     * Update instance variables with task output
     */
    private void updateInstanceVariables(WorkflowInstance instance, Map<String, Object> outputVariables) {
        if (outputVariables == null || outputVariables.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : outputVariables.entrySet()) {
            try {
                String value = (entry.getValue() instanceof String)
                        ? (String) entry.getValue()
                        : objectMapper.writeValueAsString(entry.getValue());

                instance.getVariables().put(entry.getKey(), value);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize output variable {}: {}", entry.getKey(), e.getMessage());
                instance.getVariables().put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        instanceRepository.save(instance);
    }

    /**
     * Evaluate condition expression
     */
    private boolean evaluateCondition(String condition, Map<String, Object> variables) {
        try {
            // Create evaluation context
            StandardEvaluationContext context = new StandardEvaluationContext();
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }

            // Parse and evaluate the expression
            Expression expression = expressionParser.parseExpression(condition);
            return Boolean.TRUE.equals(expression.getValue(context, Boolean.class));
        } catch (Exception e) {
            log.warn("Failed to evaluate condition: {}", condition, e);
            return false;
        }
    }

    /**
     * Deserialize variables from string map to object map
     */
    private Map<String, Object> deserializeVariables(Map<String, String> stringVariables) {
        Map<String, Object> variables = new HashMap<>();

        for (Map.Entry<String, String> entry : stringVariables.entrySet()) {
            String value = entry.getValue();
            Object deserializedValue;

            try {
                // Try to deserialize as JSON
                if (value.startsWith("{") || value.startsWith("[")) {
                    deserializedValue = objectMapper.readValue(value, Object.class);
                } else {
                    deserializedValue = value;
                }
            } catch (Exception e) {
                deserializedValue = value;
            }

            variables.put(entry.getKey(), deserializedValue);
        }

        return variables;
    }
}
