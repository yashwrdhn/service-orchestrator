package com.poc.orchestrator;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.orchestrator.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class BpmnParser {

    private ObjectMapper objectMapper;

    /**
     * Parse BPMN XML into a workflow graph
     */
    public WorkflowGraph parse(String bpmnXml) {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader xtr = xif.createXMLStreamReader(
                    new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));

            BpmnXMLConverter converter = new BpmnXMLConverter();
            BpmnModel bpmnModel = converter.convertToBpmnModel(xtr);

            if (bpmnModel.getProcesses().isEmpty()) {
                throw new IllegalArgumentException("No process found in BPMN");
            }

            Process process = bpmnModel.getMainProcess();
            return convertToWorkflowGraph(process);
        } catch (Exception e) {
            log.error("Failed to parse BPMN XML", e);
            throw new RuntimeException("Failed to parse BPMN XML: " + e.getMessage(), e);
        }
    }

    /**
     * Convert Activiti BPMN model to our internal workflow graph
     */
    private WorkflowGraph convertToWorkflowGraph(Process process) {
        WorkflowGraph graph = new WorkflowGraph();

        // Process all nodes
        for (FlowElement element : process.getFlowElements()) {
            if (element instanceof StartEvent) {
                processStartEvent(graph, (StartEvent) element);
            } else if (element instanceof ServiceTask) {
                processServiceTask(graph, (ServiceTask) element);
            } else if (element instanceof ParallelGateway) {
                processParallelGateway(graph, (ParallelGateway) element);
            } else if (element instanceof EndEvent) {
                processEndEvent(graph, (EndEvent) element);
            } else if (element instanceof SequenceFlow) {
                processSequenceFlow(graph, (SequenceFlow) element);
            }
        }

        return graph;
    }

    private void processStartEvent(WorkflowGraph graph, StartEvent startEvent) {
        WorkflowNode node = WorkflowNode.builder()
                .id(startEvent.getId())
                .name(startEvent.getName())
                .type(NodeType.START_EVENT)
                .properties(extractElementProperties(startEvent))
                .build();

        graph.addNode(node);
    }

    private void processServiceTask(WorkflowGraph graph, ServiceTask serviceTask) {
        WorkflowNode node = WorkflowNode.builder()
                .id(serviceTask.getId())
                .name(serviceTask.getName())
                .type(NodeType.SERVICE_TASK)
                .properties(extractElementProperties(serviceTask))
                .taskConfig(extractTaskConfig(serviceTask))
                .build();

        graph.addNode(node);
    }

    private void processParallelGateway(WorkflowGraph graph, ParallelGateway gateway) {
        WorkflowNode node = WorkflowNode.builder()
                .id(gateway.getId())
                .name(gateway.getName())
                .type(NodeType.PARALLEL_GATEWAY)
                .properties(extractElementProperties(gateway))
                .build();

        graph.addNode(node);
    }

    private void processEndEvent(WorkflowGraph graph, EndEvent endEvent) {
        WorkflowNode node = WorkflowNode.builder()
                .id(endEvent.getId())
                .name(endEvent.getName())
                .type(NodeType.END_EVENT)
                .properties(extractElementProperties(endEvent))
                .build();

        graph.addNode(node);
    }

    private void processSequenceFlow(WorkflowGraph graph, SequenceFlow flow) {
        WorkflowEdge edge = WorkflowEdge.builder()
                .id(flow.getId())
                .sourceNodeId(flow.getSourceRef())
                .targetNodeId(flow.getTargetRef())
                .condition(flow.getConditionExpression())
                .build();

        graph.addEdge(edge);
    }

    /**
     * Extract element properties from flow element
     */
    private Map<String, Object> extractElementProperties(FlowElement element) {
        Map<String, Object> properties = new HashMap<>();

        // Add basic properties
        properties.put("name", element.getName());
        properties.put("documentation", element.getDocumentation());

        // Add any extension elements
        if (element.getExtensionElements() != null && !element.getExtensionElements().isEmpty()) {
            for (Map.Entry<String, List<ExtensionElement>> entry : element.getExtensionElements().entrySet()) {
                List<ExtensionElement> elements = entry.getValue();
                if (elements != null && !elements.isEmpty()) {
                    for (ExtensionElement extensionElement : elements) {
                        // Extract custom properties from extension elements
                        Map<String, Object> extensionAttributes = new HashMap<>();
                        for (Map.Entry<String, List<ExtensionAttribute>> attrEntry : extensionElement.getAttributes().entrySet()) {
                            List<ExtensionAttribute> attributes = attrEntry.getValue();
                            if (attributes != null && !attributes.isEmpty()) {
                                extensionAttributes.put(attrEntry.getKey(), attributes.get(0).getValue());
                            }
                        }
                        properties.put(extensionElement.getName(), extensionAttributes);
                    }
                }
            }
        }

        // input parsing and output mapping

        return properties;
    }

    /**
     * Extract task configuration from service task
     */
    private TaskConfig extractTaskConfig(ServiceTask serviceTask) {
        // Find taskConfig extension element
        if (serviceTask.getExtensionElements() != null) {
            List<ExtensionElement> taskConfigElements = serviceTask.getExtensionElements().get("taskConfig");
            if (taskConfigElements != null && !taskConfigElements.isEmpty()) {
                try {
                    ExtensionElement configElement = taskConfigElements.get(0);

                    // Extract task type
                    String taskTypeStr = getExtensionElementValue(configElement, "type");
                    TaskType taskType = TaskType.valueOf(taskTypeStr);

                    TaskConfig.TaskConfigBuilder builder = TaskConfig.builder()
                            .type(taskType);

                    // Extract common properties
                    if (taskType == TaskType.REST) {
                        builder.endpoint(getExtensionElementValue(configElement, "endpoint"))
                                .method(getExtensionElementValue(configElement, "method"))
                                .requestBodyTemplate(getExtensionElementValue(configElement, "requestBody"));

                        // Extract response mapping
                        ExtensionElement responseMappingElement = getChildElement(configElement, "responseMapping");
                        if (responseMappingElement != null) {
                            Map<String, String> responseMapping = new HashMap<>();
                            List<ExtensionElement> outputVars = responseMappingElement.getChildElements().get("outputVar");
                            if (outputVars != null) {
                                for (ExtensionElement outputVar : outputVars) {
                                    String name = outputVar.getAttributes().get("name").get(0).getValue();
                                    String path = outputVar.getAttributes().get("path").get(0).getValue();
                                    responseMapping.put(name, path);
                                }
                            }
                            builder.responseMapping(responseMapping);
                        }
                    } else if (taskType == TaskType.KAFKA) {
                        builder.topic(getExtensionElementValue(configElement, "topic"))
                                .messageTemplate(getExtensionElementValue(configElement, "message"));
                    }

                    // Extract retry config
                    ExtensionElement retryConfigElement = getChildElement(configElement, "retryConfig");
                    if (retryConfigElement != null) {
                        RetryConfig retryConfig = RetryConfig.builder()
                                .maxAttempts(Integer.parseInt(getExtensionElementValue(retryConfigElement, "maxAttempts")))
                                .backoffPolicy(BackoffPolicy.valueOf(getExtensionElementValue(retryConfigElement, "backoffPolicy")))
                                .initialDelay(Long.parseLong(getExtensionElementValue(retryConfigElement, "initialDelay")))
                                .maxDelay(Long.parseLong(getExtensionElementValue(retryConfigElement, "maxDelay", "60000")))
                                .build();
                        builder.retryConfig(retryConfig);
                    }

                    return builder.build();
                } catch (Exception e) {
                    log.error("Failed to parse task config for {}", serviceTask.getId(), e);
                }
            }
        }

        // Default config if none found
        return TaskConfig.builder()
                .type(TaskType.REST)
                .retryConfig(RetryConfig.builder()
                        .maxAttempts(3)
                        .backoffPolicy(BackoffPolicy.FIXED)
                        .initialDelay(1000)
                        .maxDelay(10000)
                        .build())
                .build();
    }

    private String getExtensionElementValue(ExtensionElement element, String childName) {
        return getExtensionElementValue(element, childName, null);
    }

    private String getExtensionElementValue(ExtensionElement element, String childName, String defaultValue) {
        Map<String, List<ExtensionElement>> childElements = element.getChildElements();
        if (childElements != null && childElements.containsKey(childName)) {
            List<ExtensionElement> elements = childElements.get(childName);
            if (elements != null && !elements.isEmpty()) {
                return elements.get(0).getElementText();
            }
        }
        return defaultValue;
    }

    private ExtensionElement getChildElement(ExtensionElement parent, String childName) {
        Map<String, List<ExtensionElement>> childElements = parent.getChildElements();
        if (childElements != null && childElements.containsKey(childName)) {
            List<ExtensionElement> elements = childElements.get(childName);
            if (elements != null && !elements.isEmpty()) {
                return elements.get(0);
            }
        }
        return null;
    }
}
