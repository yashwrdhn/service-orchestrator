package com.poc.orchestrator.Service;

import com.poc.orchestrator.executor.RestTaskExecutor;
import com.poc.orchestrator.model.BackoffPolicy;
import com.poc.orchestrator.model.RetryConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;



@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTaskScheduler {
    private TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(WorkflowTaskScheduler.class);

    /**
     * Schedule a task for execution after a delay
     */
    public void scheduleTask(String taskId, Runnable task, long delayMs) {
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
                wrapTask(taskId, task),
                new Date(System.currentTimeMillis() + delayMs)
        );

        scheduledTasks.put(taskId, scheduledTask);
    }

    /**
     * Schedule a task with retry logic
     */
    public void scheduleWithRetry(String taskId, Supplier<Boolean> task, RetryConfig retryConfig, int currentAttempt) {
        if (currentAttempt >= retryConfig.getMaxAttempts()) {
            log.error("Task {} exceeded maximum retry attempts: {}", taskId, retryConfig.getMaxAttempts());
            return;
        }

        long delay = calculateBackoff(retryConfig, currentAttempt);

        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(() -> {
            log.info("Executing retry attempt {} for task {}", currentAttempt + 1, taskId);
            try {
                Boolean success = task.get();
                if (Boolean.FALSE.equals(success)) {
                    // Schedule next retry if task returns false
                    scheduleWithRetry(taskId, task, retryConfig, currentAttempt + 1);
                }
            } catch (Exception e) {
                log.error("Failed retry attempt {} for task {}", currentAttempt + 1, taskId, e);
                // Schedule next retry after exception
                scheduleWithRetry(taskId, task, retryConfig, currentAttempt + 1);
            } finally {
                scheduledTasks.remove(taskId);
            }
        }, new Date(System.currentTimeMillis() + delay));

        scheduledTasks.put(taskId, scheduledTask);
    }

    /**
     * Cancel scheduled task
     */
    public void cancelTask(String taskId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.remove(taskId);
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }
    }

    /**
     * Calculate backoff delay based on policy
     */
    private long calculateBackoff(RetryConfig retryConfig, int attempt) {
        BackoffPolicy policy = retryConfig.getBackoffPolicy();
        long delay;

        switch (policy) {
            case FIXED:
                delay = retryConfig.getInitialDelay();
                break;
            case LINEAR:
                delay = retryConfig.getInitialDelay() * (attempt + 1);
                break;
            case EXPONENTIAL:
                delay = retryConfig.getInitialDelay() * (long) Math.pow(2, attempt);
                break;
            default:
                delay = retryConfig.getInitialDelay();
        }

        return Math.min(delay, retryConfig.getMaxDelay());
    }

    /**
     * Wrap task with error handling and cleanup
     */
    private Runnable wrapTask(String taskId, Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Task execution failed: {}", taskId, e);
            } finally {
                scheduledTasks.remove(taskId);
            }
        };
    }
}