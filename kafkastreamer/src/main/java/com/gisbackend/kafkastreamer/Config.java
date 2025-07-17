package com.gisbackend.kafkastreamer;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.gisbackend.kafkastreamer.model.GraphDataModelsWrapper;

@Configuration
@EnableKafka
public class Config {
	
    @Bean
    public ConsumerFactory<String, GraphDataModelsWrapper> graphModelConsumer() {
        Map<String, Object> props = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
            ConsumerConfig.GROUP_ID_CONFIG, "gis_group",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
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
        return factory;
    }
}
