package ru.t1.java.demo.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import ru.t1.java.demo.model.dto.TransactionDto;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaTransactionProducer {

    @Autowired
    private KafkaTemplate<String, TransactionDto> transactionDtoKafkaTemplate;
    @Value("${client_transactions_errors}")
    private String clientTransactionErrorTopic;
    @Value("${client_transactions}")
    private String clientTransactionsTopic;


    public void sendTransactionErrorMessage(TransactionDto transactionDto) {
        transactionDtoKafkaTemplate.send(clientTransactionErrorTopic, transactionDto.getTransactionId(), transactionDto);
    }
    public void sendTransactionMessage(TransactionDto transactionDto, String action) {
        Message<TransactionDto> message = MessageBuilder.withPayload(transactionDto)
                .setHeader(KafkaHeaders.TOPIC, clientTransactionsTopic)
                .setHeader(KafkaHeaders.KEY, transactionDto.getTransactionId())
                .setHeader("action", action)
                .build();
        transactionDtoKafkaTemplate.send(message);
    }

}
