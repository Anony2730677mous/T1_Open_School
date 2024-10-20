package ru.t1.java.demo.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

public class KafkaAspectMessageProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaAspectMessageProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, String>> sendAspectMessage(String topic, String message){
        return kafkaTemplate.send(topic, message);
    }
}
