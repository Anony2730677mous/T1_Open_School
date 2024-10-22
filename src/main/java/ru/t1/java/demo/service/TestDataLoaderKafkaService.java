package ru.t1.java.demo.service;

import com.github.javafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.t1.java.demo.kafka.KafkaTestDataProducer;
import ru.t1.java.demo.model.ClientAccountType;
import ru.t1.java.demo.model.dto.ClientAccountDto;
import ru.t1.java.demo.model.dto.TransactionDto;

import java.math.BigDecimal;
import java.util.stream.IntStream;

@Service
public class TestDataLoaderKafkaService {
    @Autowired
    private KafkaTestDataProducer kafkaTestDataProducer;

    private Faker faker = new Faker();

    public void loadTestClientAccountData() {
        IntStream.range(0, 100).forEach(i -> {
            ClientAccountDto clientAccountDto = new ClientAccountDto();
            clientAccountDto.setClientId(faker.number().randomNumber());
            clientAccountDto.setClientAccountType(ClientAccountType.CREDIT);
            clientAccountDto.setBalance(BigDecimal.valueOf(faker.number().randomDouble(2, 100, 10000)));
            kafkaTestDataProducer.sendClientAccountMessage(clientAccountDto);
        });
    }


    public void loadTestTransactionData(){
        IntStream.range(0, 100).forEach(i -> {
            TransactionDto transactionDto = new TransactionDto();
            transactionDto.setTransactionId(faker.idNumber().valid());
            transactionDto.setClientId(faker.number().randomNumber());
            transactionDto.setAccountId(faker.number().randomNumber());
            transactionDto.setAmount(BigDecimal.valueOf(faker.number().randomDouble(2, 100, 10000)));
            kafkaTestDataProducer.sendTransactionMessage(transactionDto);
        });
    }
}
