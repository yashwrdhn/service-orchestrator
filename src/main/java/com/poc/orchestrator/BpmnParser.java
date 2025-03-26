package com.poc.orchestrator;
//
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.poc.orchestrator.model.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.activiti.bpmn.converter.BpmnXMLConverter;
//import org.activiti.bpmn.model.*;
//import org.activiti.bpmn.model.Process;
//import org.springframework.stereotype.Component;
//
//import javax.xml.stream.XMLInputFactory;
//import javax.xml.stream.XMLStreamReader;
//import java.io.ByteArrayInputStream;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class BpmnParser {
//
//    private ObjectMapper objectMapper;
//
//    /**
//     * Parse BPMN XML into a workflow graph
//     */
//    public WorkflowGraph parse(String bpmnXml) {
//        try {
//            XMLInputFactory xif = XMLInputFactory.newInstance();
//            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
//            XMLStreamReader xtr = xif.createXMLStreamReader(
//                    new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));
//
//            BpmnXMLConverter converter = new BpmnXMLConverter();
//            BpmnModel bpmnModel = converter.convertToBpmnModel(xtr);
//
//            if (bpmnModel.getProcesses().isEmpty()) {
//                throw new IllegalArgumentException("No process found in BPMN");
//            }
//
//            Process process = bpmnModel.getMainProcess();
//            return convertToWorkflowGraph(process);
//        } catch (Exception e) {
//            log.error("Failed to parse BPMN XML", e);
//            throw new RuntimeException("Failed to parse BPMN XML: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Convert Activiti BPMN model to our internal workflow graph
//     */
//    private WorkflowGraph convertToWorkflowGraph(Process process) {
//        WorkflowGraph graph = new WorkflowGraph();
//
//        // Process all nodes
//        for (FlowElement element : process.getFlowElements()) {
//            if (element instanceof StartEvent) {
//                processStartEvent(graph, (StartEvent) element);
//            } else if (element instanceof ServiceTask) {
//                processServiceTask(graph, (ServiceTask) element);
//            } else if (element instanceof ParallelGateway) {
//                processParallelGateway(graph, (ParallelGateway) element);
//            } else if (element instanceof EndEvent) {
//                processEndEvent(graph, (EndEvent) element);
//            } else if (element instanceof SequenceFlow) {
//                processSequenceFlow(graph, (SequenceFlow) element);
//            }
//        }
//
//        return graph;
//    }
//
//    private void processStartEvent(WorkflowGraph graph, StartEvent startEvent) {
//        WorkflowNode node = WorkflowNode.builder()
//                .id(startEvent.getId())
//                .name(startEvent.getName())
//                .type(NodeType.START_EVENT)
//                .properties(extractElementProperties(startEvent))
//                .build();
//
//        graph.addNode(node);
//    }
//
//    private void processServiceTask(WorkflowGraph graph, ServiceTask serviceTask) {
//        WorkflowNode node = WorkflowNode.builder()
//                .id(serviceTask.getId())
//                .name(serviceTask.getName())
//                .type(NodeType.SERVICE_TASK)
//                .properties(extractElementProperties(serviceTask))
//                .taskConfig(extractTaskConfig(serviceTask))
//                .build();
//
//        graph.addNode(node);
//    }
//
//    private void processParallelGateway(WorkflowGraph graph, ParallelGateway gateway) {
//        WorkflowNode node = WorkflowNode.builder()
//                .id(gateway.getId())
//                .name(gateway.getName())
//                .type(NodeType.PARALLEL_GATEWAY)
//                .properties(extractElementProperties(gateway))
//                .build();
//
//        graph.addNode(node);
//    }
//
//    private void processEndEvent(WorkflowGraph graph, EndEvent endEvent) {
//        WorkflowNode node = WorkflowNode.builder()
//                .id(endEvent.getId())
//                .name(endEvent.getName())
//                .type(NodeType.END_EVENT)
//                .properties(extractElementProperties(endEvent))
//                .build();
//
//        graph.addNode(node);
//    }
//
//    private void processSequenceFlow(WorkflowGraph graph, SequenceFlow flow) {
//        WorkflowEdge edge = WorkflowEdge.builder()
//                .id(flow.getId())
//                .sourceNodeId(flow.getSourceRef())
//                .targetNodeId(flow.getTargetRef())
//                .condition(flow.getConditionExpression())
//                .build();
//
//        graph.addEdge(edge);
//    }
//
//    /**
//     * Extract element properties from flow element
//     */
//    private Map<String, Object> extractElementProperties(FlowElement element) {
//        Map<String, Object> properties = new HashMap<>();
//
//        // Add basic properties
//        properties.put("name", element.getName());
//        properties.put("documentation", element.getDocumentation());
//
//        // Add any extension elements
//        if (element.getExtensionElements() != null && !element.getExtensionElements().isEmpty()) {
//            for (Map.Entry<String, List<ExtensionElement>> entry : element.getExtensionElements().entrySet()) {
//                List<ExtensionElement> elements = entry.getValue();
//                if (elements != null && !elements.isEmpty()) {
//                    for (ExtensionElement extensionElement : elements) {
//                        // Extract custom properties from extension elements
//                        Map<String, Object> extensionAttributes = new HashMap<>();
//                        for (Map.Entry<String, List<ExtensionAttribute>> attrEntry : extensionElement.getAttributes().entrySet()) {
//                            List<ExtensionAttribute> attributes = attrEntry.getValue();
//                            if (attributes != null && !attributes.isEmpty()) {
//                                extensionAttributes.put(attrEntry.getKey(), attributes.get(0).getValue());
//                            }
//                        }
//                        properties.put(extensionElement.getName(), extensionAttributes);
//                    }
//                }
//            }
//        }
//
//        // input parsing and output mapping
//
//        return properties;
//    }
//
//    /**
//     * Extract task configuration from service task
//     */
//    private TaskConfig extractTaskConfig(ServiceTask serviceTask) {
//        // Find taskConfig extension element
//        if (serviceTask.getExtensionElements() != null) {
//            List<ExtensionElement> taskConfigElements = serviceTask.getExtensionElements().get("taskConfig");
//            if (taskConfigElements != null && !taskConfigElements.isEmpty()) {
//                try {
//                    ExtensionElement configElement = taskConfigElements.get(0);
//
//                    // Extract task type
//                    String taskTypeStr = getExtensionElementValue(configElement, "type");
//                    TaskType taskType = TaskType.valueOf(taskTypeStr);
//
//                    TaskConfig.TaskConfigBuilder builder = TaskConfig.builder()
//                            .type(taskType);
//
//                    // Extract common properties
//                    if (taskType == TaskType.REST) {
//                        builder.endpoint(getExtensionElementValue(configElement, "endpoint"))
//                                .method(getExtensionElementValue(configElement, "method"))
//                                .requestBodyTemplate(getExtensionElementValue(configElement, "requestBody"));
//
//                        // Extract response mapping
//                        ExtensionElement responseMappingElement = getChildElement(configElement, "responseMapping");
//                        if (responseMappingElement != null) {
//                            Map<String, String> responseMapping = new HashMap<>();
//                            List<ExtensionElement> outputVars = responseMappingElement.getChildElements().get("outputVar");
//                            if (outputVars != null) {
//                                for (ExtensionElement outputVar : outputVars) {
//                                    String name = outputVar.getAttributes().get("name").get(0).getValue();
//                                    String path = outputVar.getAttributes().get("path").get(0).getValue();
//                                    responseMapping.put(name, path);
//                                }
//                            }
//                            builder.responseMapping(responseMapping);
//                        }
//                    } else if (taskType == TaskType.KAFKA) {
//                        builder.topic(getExtensionElementValue(configElement, "topic"))
//                                .messageTemplate(getExtensionElementValue(configElement, "message"));
//                    }
//
//                    // Extract retry config
//                    ExtensionElement retryConfigElement = getChildElement(configElement, "retryConfig");
//                    if (retryConfigElement != null) {
//                        RetryConfig retryConfig = RetryConfig.builder()
//                                .maxAttempts(Integer.parseInt(getExtensionElementValue(retryConfigElement, "maxAttempts")))
//                                .backoffPolicy(BackoffPolicy.valueOf(getExtensionElementValue(retryConfigElement, "backoffPolicy")))
//                                .initialDelay(Long.parseLong(getExtensionElementValue(retryConfigElement, "initialDelay")))
//                                .maxDelay(Long.parseLong(getExtensionElementValue(retryConfigElement, "maxDelay", "60000")))
//                                .build();
//                        builder.retryConfig(retryConfig);
//                    }
//
//                    return builder.build();
//                } catch (Exception e) {
//                    log.error("Failed to parse task config for {}", serviceTask.getId(), e);
//                }
//            }
//        }
//
//        // Default config if none found
//        return TaskConfig.builder()
//                .type(TaskType.REST)
//                .retryConfig(RetryConfig.builder()
//                        .maxAttempts(3)
//                        .backoffPolicy(BackoffPolicy.FIXED)
//                        .initialDelay(1000)
//                        .maxDelay(10000)
//                        .build())
//                .build();
//    }
//
//    private String getExtensionElementValue(ExtensionElement element, String childName) {
//        return getExtensionElementValue(element, childName, null);
//    }
//
//    private String getExtensionElementValue(ExtensionElement element, String childName, String defaultValue) {
//        Map<String, List<ExtensionElement>> childElements = element.getChildElements();
//        if (childElements != null && childElements.containsKey(childName)) {
//            List<ExtensionElement> elements = childElements.get(childName);
//            if (elements != null && !elements.isEmpty()) {
//                return elements.get(0).getElementText();
//            }
//        }
//        return defaultValue;
//    }
//
//    private ExtensionElement getChildElement(ExtensionElement parent, String childName) {
//        Map<String, List<ExtensionElement>> childElements = parent.getChildElements();
//        if (childElements != null && childElements.containsKey(childName)) {
//            List<ExtensionElement> elements = childElements.get(childName);
//            if (elements != null && !elements.isEmpty()) {
//                return elements.get(0);
//            }
//        }
//        return null;
//    }
//}



import com.poc.orchestrator.model.NodeType;
import com.poc.orchestrator.model.WorkflowEdge;
import com.poc.orchestrator.model.WorkflowGraph;
import com.poc.orchestrator.model.WorkflowNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import org.xml.sax.InputSource;


import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class BpmnParser {
    private static final Logger log = LoggerFactory.getLogger(BpmnParser.class);

    public WorkflowGraph parse(String bpmnXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(bpmnXml)));

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

           /*
            xpath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    switch(prefix) {
                        case "bpmn": return "http://www.omg.org/spec/BPMN/20100524/MODEL";
                        case "camunda": return "http://camunda.org/schema/1.0/bpmn";
                        default: return null;
                    }
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    if ("http://www.omg.org/spec/BPMN/20100524/MODEL".equals(namespaceURI))
                        return "bpmn";
                    if ("http://camunda.org/schema/1.0/bpmn".equals(namespaceURI))
                        return "camunda";
                    return null;
                }

                @Override
                public Iterator<String> getPrefixes(String namespaceURI) {
                    List<String> prefixes = new ArrayList<>();
                    if ("http://www.omg.org/spec/BPMN/20100524/MODEL".equals(namespaceURI))
                        prefixes.add("bpmn");
                    if ("http://camunda.org/schema/1.0/bpmn".equals(namespaceURI))
                        prefixes.add("camunda");
                    return prefixes.iterator();
                }
            });
            */
            printDocumentStructure(document);
            WorkflowGraph graph = new WorkflowGraph();

            // Parse nodes
            parseNodes(document, xpath, graph);

            // Parse edges
            parseEdges(document, graph);


            return graph;
        } catch (Exception e) {
            log.error("Failed to parse BPMN XML", e);
            throw new RuntimeException("Failed to parse BPMN XML: " + e.getMessage(), e);
        }
    }

    private void printDocumentStructure(Document document) {
        try {
            // Print the entire XML document for debugging
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(source, result);
            String xmlString = result.getWriter().toString();
            log.info("Full XML Document:\n{}", xmlString);

            // Attempt to print namespaces
            Element documentElement = document.getDocumentElement();
            log.info("Document Element: {}", documentElement.getNodeName());

            // Log all declared namespaces
            NamedNodeMap attributes = documentElement.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                if (attr.getNodeName().startsWith("xmlns:")) {
                    log.info("Namespace Prefix: {}, URI: {}",
                            attr.getNodeName(),
                            attr.getNodeValue()
                    );
                }
            }
        } catch (Exception e) {
            log.error("Error printing document structure", e);
        }
    }

    private void parseNodes(Document document, XPath xpath, WorkflowGraph graph) throws Exception {
        // Try multiple XPath expressions
        String[] nodeQueries = {
                "//bpmn:startEvent",
                "//*[local-name()='startEvent']",
                "//startEvent",
                "//bpmn:process/bpmn:startEvent"
        };

        for (String query : nodeQueries) {
            try {
                log.info("Trying XPath query for start events: {}", query);
                NodeList startEvents = (NodeList) xpath.evaluate(query, document, XPathConstants.NODESET);

                log.info("Found {} start events with query: {}", startEvents.getLength(), query);

                for (int i = 0; i < startEvents.getLength(); i++) {
                    org.w3c.dom.Node eventNode = startEvents.item(i);
                    log.info("Start Event Node: {}", eventNode.getNodeName());
                    WorkflowNode node = createWorkflowNode(eventNode, NodeType.START_EVENT);
                    graph.addNode(node);
                }

                if (startEvents.getLength() > 0) break;
            } catch (Exception e) {
                log.error("Error with XPath query: {}", query, e);
            }
        }

        // Similar approach for service tasks
        String[] serviceTaskQueries = {
                "//bpmn:serviceTask",
                "//*[local-name()='serviceTask']",
                "//serviceTask",
                "//bpmn:process/bpmn:serviceTask"
        };

        for (String query : serviceTaskQueries) {
            try {
                log.info("Trying XPath query for service tasks: {}", query);
                NodeList serviceTasks = (NodeList) xpath.evaluate(query, document, XPathConstants.NODESET);

                log.info("Found {} service tasks with query: {}", serviceTasks.getLength(), query);

                for (int i = 0; i < serviceTasks.getLength(); i++) {
                    org.w3c.dom.Node taskNode = serviceTasks.item(i);
                    log.info("Service Task Node: {}", taskNode.getNodeName());
                    WorkflowNode node = createWorkflowNode(taskNode, NodeType.SERVICE_TASK);

                    // Extract custom connector details
                    Map<String, Object> connectorDetails = extractConnectorDetails(taskNode);
                    if (!connectorDetails.isEmpty()) {
                        node.getProperties().put("connector", connectorDetails);
                    }

                    graph.addNode(node);
                }

                if (serviceTasks.getLength() > 0) break;
            } catch (Exception e) {
                log.error("Error with XPath query: {}", query, e);
            }
        }
    }

    private void parseEdges(Document document, WorkflowGraph graph) {
        String[] EdgeQueries = {
                "//bpmn:sequenceFlow",
                "//*[local-name()='sequenceFlow']",
                "//sequenceFlow",
                "//bpmn:process/bpmn:sequenceFlow"
        };
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            // Parse sequence flows
            NodeList sequenceFlows = (NodeList) xpath.evaluate(EdgeQueries[1], document, XPathConstants.NODESET);
            for (int i = 0; i < sequenceFlows.getLength(); i++) {
                org.w3c.dom.Node flowNode = sequenceFlows.item(i);

                String id = flowNode.getAttributes().getNamedItem("id").getNodeValue();
                String sourceRef = flowNode.getAttributes().getNamedItem("sourceRef").getNodeValue();
                String targetRef = flowNode.getAttributes().getNamedItem("targetRef").getNodeValue();

                WorkflowEdge edge = WorkflowEdge.builder()
                        .id(id)
                        .sourceNodeId(sourceRef)
                        .targetNodeId(targetRef)
                        .build();

                graph.addEdge(edge);
            }
        } catch (Exception e) {
            log.error("Error parsing edges", e);
            throw new RuntimeException("Error parsing edges", e);
        }
    }

    private WorkflowNode createWorkflowNode(org.w3c.dom.Node node, NodeType type) {
        NamedNodeMap attributes = node.getAttributes();
        String id = attributes.getNamedItem("id").getNodeValue();
        String name = attributes.getNamedItem("name") != null
                ? attributes.getNamedItem("name").getNodeValue()
                : id;

        System.out.println("name : " + name + "id: " + id);

        return WorkflowNode.builder()
                .id(id)
                .name(name)
                .type(type)
                .build();
    }

    private Map<String, Object> extractConnectorDetails(org.w3c.dom.Node taskNode) {
        Map<String, Object> connectorDetails = new HashMap<>();
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList connectorElements = (NodeList) xpath.evaluate(
                    ".//camunda:connector",
                    taskNode,
                    XPathConstants.NODESET
            );

            if (connectorElements.getLength() > 0) {
                org.w3c.dom.Node connectorNode = connectorElements.item(0);

                // Extract connector ID
                NodeList connectorIdNodes = (NodeList) xpath.evaluate(
                        ".//camunda:connectorId",
                        connectorNode,
                        XPathConstants.NODESET
                );
                if (connectorIdNodes.getLength() > 0) {
                    connectorDetails.put("connectorId",
                            connectorIdNodes.item(0).getTextContent());
                }

                // Extract input parameters
                NodeList inputParamNodes = (NodeList) xpath.evaluate(
                        ".//camunda:inputParameter",
                        connectorNode,
                        XPathConstants.NODESET
                );
                Map<String, String> inputParams = new HashMap<>();
                for (int i = 0; i < inputParamNodes.getLength(); i++) {
                    org.w3c.dom.Node paramNode = inputParamNodes.item(i);
                    String name = paramNode.getAttributes().getNamedItem("name").getNodeValue();
                    String value = paramNode.getTextContent();
                    inputParams.put(name, value);
                }
                connectorDetails.put("inputParameters", inputParams);
            }
        } catch (Exception e) {
            log.error("Error extracting connector details", e);
        }
        return connectorDetails;
    }
}