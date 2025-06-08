package io.alw.css.cashflowconsumer.config;

import io.alw.css.serialization.cashflow.FoCashMessageAvro;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FoCashMessageAvro> foCashMessageListenerContainerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties(null);
        DefaultKafkaConsumerFactory<String, FoCashMessageAvro> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProperties);

        ConcurrentKafkaListenerContainerFactory<String, FoCashMessageAvro> listenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        listenerContainerFactory.setConsumerFactory(consumerFactory);
        return listenerContainerFactory;
    }
}
