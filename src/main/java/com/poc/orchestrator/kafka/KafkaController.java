package com.poc.orchestrator.kafka;

import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.apache.kafka.streams.KafkaStreams;

@RestController
@RequestMapping("/kafka")
public class KafkaController {
    private final KafkaProducer kafkaProducer;
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public KafkaController(KafkaProducer kafkaProducer, StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.kafkaProducer = kafkaProducer;
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    @PostMapping("/send/{message}")
    public String sendMessage(@PathVariable String message) {
        kafkaProducer.sendMessage("test-topic", message);
        return "Message sent!";
    }

    @GetMapping("/{taskId}")
    public String getTaskState(@PathVariable String taskId) {
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();

        if (streams == null) {
            return "Kafka Streams is not initialized yet.";
        }

        ReadOnlyKeyValueStore<String, String> store =
                streams.store(StoreQueryParameters.fromNameAndType("task-state-store", QueryableStoreTypes.keyValueStore()));

        return store.get(taskId);
    }
}