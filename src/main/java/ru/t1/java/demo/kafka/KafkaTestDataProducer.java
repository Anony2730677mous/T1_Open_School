package ru.t1.java.demo.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.t1.java.demo.model.dto.ClientAccountDto;
import ru.t1.java.demo.model.dto.TransactionDto;
@Service
public class KafkaTestDataProducer {
    @Autowired
    private KafkaTemplate<String, ClientAccountDto> clientAccountDtoKafkaTemplate;
    @Autowired
    private KafkaTemplate<String, TransactionDto> transactionDtoKafkaTemplate;

    @Value("${t1.kafka.topic.client_new_account_registration}")
    private String clientAccountsTopic;

    @Value("${t1.kafka.topic.client_transactions}")
    private String clientTransactionsTopic;
    public void sendClientAccountMessage(ClientAccountDto clientAccountDto) {
        clientAccountDtoKafkaTemplate.send(clientAccountsTopic, clientAccountDto.getClientId().toString(), clientAccountDto);
    }

    public void sendTransactionMessage(TransactionDto transactionDto) {
        transactionDtoKafkaTemplate.send(clientTransactionsTopic, transactionDto.getTransactionId(), transactionDto);
    }
}
