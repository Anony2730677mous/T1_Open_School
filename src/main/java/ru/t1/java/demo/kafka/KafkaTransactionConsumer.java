package ru.t1.java.demo.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.t1.java.demo.mapper.TransactionMapper;
import ru.t1.java.demo.model.Transaction;
import ru.t1.java.demo.model.dto.TransactionDto;
import ru.t1.java.demo.service.TransactionService;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaTransactionConsumer {
    private final TransactionMapper transactionMapper;
    private final TransactionService transactionService;

    @KafkaListener(id = "${t1.kafka.consumer.group-id}",
            topics = "${t1.kafka.topic.client_transactions}",
            containerFactory = "kafkaListenerContainerFactory")
    public void transactionListener(@Payload TransactionDto transactionDto,
                                    Acknowledgment ack,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            log.info("Сообщение получено из топика: {}, с ключом: {}", topic, key);
            Transaction transaction = transactionMapper.toEntity(transactionDto);
            transactionService.saveTransaction(transaction);
        } catch (Exception e) {
            log.error("Произошла ошибка во время транзакции: " + e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
