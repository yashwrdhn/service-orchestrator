//package com.poc.orchestrator.executor;
//
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.poc.orchestrator.model.TaskConfig;
//import com.poc.orchestrator.model.TaskResult;
//import com.poc.orchestrator.util.ExpressionEvaluator;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.SendResult;
//import org.springframework.stereotype.Component;
//import org.springframework.util.concurrent.ListenableFuture;
//import org.springframework.util.concurrent.ListenableFutureCallback;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class KafkaTaskExecutor implements TaskExecutor {
//    private KafkaTemplate<String, String> kafkaTemplate;
//    private ObjectMapper objectMapper;
//    private ExpressionEvaluator expressionEvaluator;
//
//    private static final Logger log = LoggerFactory.getLogger(KafkaTaskExecutor.class);
//
//    @Override
//    public CompletableFuture<TaskResult> execute(TaskConfig config, Map<String, Object> variables) {
//        CompletableFuture<TaskResult> resultFuture = new CompletableFuture<>();
//
//        try {
//            // Evaluate message template with current variables
//            String message = expressionEvaluator.evaluate(config.getMessageTemplate(), variables);
//
//            // Send message to Kafka topic
//            ListenableFuture<SendResult<String, String>> sendFuture =
//                    kafkaTemplate.send(config.getTopic(), message);
//
//            // Handle result asynchronously
//            sendFuture.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
//                @Override
//                public void onSuccess(SendResult<String, String> result) {
//                    log.info("Message sent to topic {} with offset {}",
//                            config.getTopic(), result.getRecordMetadata().offset());
//
//                    Map<String, Object> outputVariables = new HashMap<>();
//                    outputVariables.put("topic", config.getTopic());
//                    outputVariables.put("offset", result.getRecordMetadata().offset());
//                    outputVariables.put("partition", result.getRecordMetadata().partition());
//                    outputVariables.put("timestamp", result.getRecordMetadata().timestamp());
//
//                    resultFuture.complete(TaskResult.success(outputVariables));
//                }
//
//                @Override
//                public void onFailure(Throwable ex) {
//                    log.error("Failed to send message to topic {}", config.getTopic(), ex);
//                    resultFuture.complete(TaskResult.builder()
//                            .status(TaskStatus.FAILURE)
//                            .outputVariables(Map.of("error", ex.getMessage()))
//                            .build());
//                }
//            });
//        } catch (Exception e) {
//            log.error("Failed to execute Kafka task", e);
//            resultFuture.complete(TaskResult.builder()
//                    .status(TaskStatus.FAILURE)
//                    .outputVariables(Map.of("error", e.getMessage()))
//                    .build());
//        }
//
//        return resultFuture;
//    }
//}