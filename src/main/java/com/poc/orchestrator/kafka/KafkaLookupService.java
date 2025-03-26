package com.poc.orchestrator.kafka;

import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

@Service
public class KafkaLookupService {
    private final KafkaStreamsConfig kafkaStreamsConfig;
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public KafkaLookupService(
            KafkaStreamsConfig kafkaStreamsConfig,
            StreamsBuilderFactoryBean streamsBuilderFactoryBean
    ) {
        this.kafkaStreamsConfig = kafkaStreamsConfig;
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

//    public String getValue(String key) {
//        return kafkaStreamsConfig.lookupByKey(key, streamsBuilderFactoryBean.getKafkaStreams());
//    }
}