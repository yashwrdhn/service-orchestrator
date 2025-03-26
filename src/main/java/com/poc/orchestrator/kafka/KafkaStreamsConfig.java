
package com.poc.orchestrator.kafka;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    private static final String TOPIC = "test-topic";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.streams.application-id}")
    private String applicationId;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration defaultKafkaStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public KStream<String, String> processKafkaStream(StreamsBuilder streamsBuilder) {
        // Create KStream from the SINGLE topic
        KStream<String, String> stream = streamsBuilder.stream(
                TOPIC,
                Consumed.with(Serdes.String(), Serdes.String())
        );

//         Create KTable from the SAME topic
        KTable<String, String> table = streamsBuilder.table(
                TOPIC,
                Materialized.as("test-topic-store")
        );

        // Example of how you might use both stream and table together
        stream.foreach((key, value) -> {
            System.out.println("Stream Record: Key=" + key + ", Value=" + value);
        });

        table.toStream().foreach((key, value) -> {
            System.out.println("Table Record: Key=" + key + ", Value=" + value);
        });

        return stream;
    }
}