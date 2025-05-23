package com.poc.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class OmLiteApplication {

	public static void main(String[] args) {
		SpringApplication.run(OmLiteApplication.class, args);
	}

}
