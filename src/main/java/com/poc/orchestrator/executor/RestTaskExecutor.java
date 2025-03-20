package com.poc.orchestrator.executor;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.poc.orchestrator.model.TaskConfig;
import com.poc.orchestrator.model.TaskResult;
import com.poc.orchestrator.model.TaskStatus;
import com.poc.orchestrator.util.ExpressionEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestTaskExecutor implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(RestTaskExecutor.class);
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private ExpressionEvaluator expressionEvaluator;

    @Override
    public CompletableFuture<TaskResult> execute(TaskConfig config, Map<String, Object> variables) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Prepare request headers
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");

                // Evaluate request body template with current variables
                String requestBody = expressionEvaluator.evaluate(config.getRequestBodyTemplate(), variables);

                // Create HTTP entity
                HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

                // Execute HTTP request
                HttpMethod method = HttpMethod.valueOf(config.getMethod());
                ResponseEntity<String> response = restTemplate.exchange(
                        config.getEndpoint(),
                        method,
                        requestEntity,
                        String.class
                );

                // Process response
                String responseBody = response.getBody();
                Map<String, Object> outputVariables = new HashMap<>();

                if (responseBody != null && config.getResponseMapping() != null) {
                    // Apply JSON path mapping
                    DocumentContext context = JsonPath.parse(responseBody);
                    for (Map.Entry<String, String> mapping : config.getResponseMapping().entrySet()) {
                        try {
                            Object value = context.read(mapping.getValue());
                            outputVariables.put(mapping.getKey(), value);
                        } catch (Exception e) {
                            log.warn("Failed to extract value for {} with path {}",
                                    mapping.getKey(), mapping.getValue(), e);
                        }
                    }
                }

                return TaskResult.success(outputVariables);
            } catch (Exception e) {
                log.error("Failed to execute REST task", e);
                return TaskResult.builder()
                        .status(TaskStatus.FAILURE)
                        .outputVariables(Map.of("error", e.getMessage()))
                        .build();
            }
        });
    }
}
