package ru.t1.java.demo.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.t1.java.demo.mapper.ClientAccountMapper;
import ru.t1.java.demo.model.ClientAccount;
import ru.t1.java.demo.model.dto.ClientAccountDto;
import ru.t1.java.demo.service.ClientAccountService;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaClientAccountConsumer {
    private final ClientAccountMapper clientAccountMapper;
    private final ClientAccountService clientAccountService;

    @KafkaListener(id = "${t1.kafka.consumer.group-id}",
            topics = "${t1.kafka.topic.client_accounts}",
            containerFactory = "kafkaListenerContainerFactory")
    public void clientAccountListener(@Payload ClientAccountDto clientAccountDto,
                                      Acknowledgment ack,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            log.info("Сообщение получено из топика: {}, с ключом: {}", topic, key);
            ClientAccount clientAccount = clientAccountMapper.toEntity(clientAccountDto);
            clientAccountService.saveAccount(clientAccount);
        } catch (Exception e) {
            log.error("Произошла ошибка при сохранении счета: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}

