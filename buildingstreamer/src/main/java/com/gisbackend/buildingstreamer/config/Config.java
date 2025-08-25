package com.gisbackend.buildingstreamer.config;

import java.util.Map;
import java.time.Duration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.web.client.RestTemplate;

import com.gisbackend.buildingstreamer.model.GraphDataModelsWrapper;

@Configuration
@EnableKafka
public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    @Value("${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}")
    private String kafkaBootstrapServers;

    @Bean
    public ConsumerFactory<String, GraphDataModelsWrapper> graphModelConsumer() {
        Map<String, Object> props = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG, "gis_group",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true,
            ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000,
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1
        );

            JsonDeserializer<GraphDataModelsWrapper> deserializer = new JsonDeserializer<>(GraphDataModelsWrapper.class);
    deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, GraphDataModelsWrapper> graphModelListener() {
        ConcurrentKafkaListenerContainerFactory<String, GraphDataModelsWrapper> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(graphModelConsumer());
        
        // Configure error handling and retry behavior
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
            (record, exception) -> {
                // Log the error but don't retry endlessly
                logger.error("Error processing record: {} - Exception: {}", record, exception.getMessage());
            },
            new org.springframework.util.backoff.FixedBackOff(0L, 0L) // No retries
        ));
        
        return factory;
    }

    @Bean("nominatimRestTemplate")
    public RestTemplate nominatimRestTemplate(RestTemplateBuilder builder) {
        logger.info("Creating RestTemplate for Nominatim with direct connection");
        
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
}
