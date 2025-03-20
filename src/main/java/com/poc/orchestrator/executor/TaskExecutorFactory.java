package com.poc.orchestrator.executor;

import com.poc.orchestrator.model.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.poc.orchestrator.model.TaskType.KAFKA;
import static com.poc.orchestrator.model.TaskType.REST;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExecutorFactory {

    private RestTaskExecutor restTaskExecutor;
//    private KafkaTaskExecutor kafkaTaskExecutor;

    public TaskExecutor getExecutor(TaskType taskType) {
        switch (taskType) {
            case REST:
                return restTaskExecutor;
//            case KAFKA:
//                return "";
//                return kafkaTaskExecutor;
            default:
                throw new IllegalArgumentException("Unsupported task type: " + taskType);
        }
    }
}
