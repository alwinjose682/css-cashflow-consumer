package io.alw.css.cashflowconsumer.service;

import io.alw.css.domain.common.InputBy;
import io.alw.css.serialization.cashflow.FoCashMessageAvro;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class KafkaCashflowConsumer {
    private final CashflowService cashflowService;

    public KafkaCashflowConsumer(CashflowService cashflowService) {
        this.cashflowService = cashflowService;
    }

    @KafkaListener(topics = "${app.kafka.topic.cashflow-input}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "foCashMessageListenerContainerFactory")
    public void accept(Message<FoCashMessageAvro> message) {
        cashflowService.process(message.getPayload(), InputBy.CSS_SYS);
    }
}
