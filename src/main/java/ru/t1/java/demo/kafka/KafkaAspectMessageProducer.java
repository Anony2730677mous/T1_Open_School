package ru.t1.java.demo.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaAspectMessageProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaAspectMessageProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendAspectMessage(String topic, String message){
        kafkaTemplate.send(topic, message);
    }
}
