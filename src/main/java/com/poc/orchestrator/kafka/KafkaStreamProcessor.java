package com.poc.orchestrator.kafka;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

public class KafkaStreamProcessor {
    @Autowired
    @Bean
    public KStream<String, String> kStreamProcessor(StreamsBuilder streamsBuilder) {


        // Create KStream from the topic
        KStream<String, String> transactionStream = streamsBuilder.stream(
                "test-topic",
                Consumed.with(Serdes.String(), Serdes.String())
        );

        // Create KTable from the same topic
        KTable<String, String> transactionTable = streamsBuilder.table(
                "test-topic",
                Materialized.with(Serdes.String(), Serdes.String())
        );

        // Example processing: Print stream and table contents
        transactionStream.foreach((key, value) ->
                System.out.println("Stream Record: Key=" + key + ", Value=" + value)
        );

        transactionTable.toStream().foreach((key, value) ->
                System.out.println("Table Record: Key=" + key + ", Value=" + value)
        );

//        // More advanced processing can be added here
//        // For example, aggregating transactions by user
//        KTable<String, Double> userTotalSpend = transactionStream
//                .groupByKey()
//                .aggregate(
//                        () -> 0.0,
//                        (key, value, aggregate) -> aggregate + Double.parseDouble(value),
//                        Materialized.with(Serdes.String(), Serdes.Double())
//                );

        return transactionStream;
    }
}
