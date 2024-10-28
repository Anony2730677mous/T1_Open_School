package ru.t1.java.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.t1.java.demo.kafka.KafkaTransactionProducer;
import ru.t1.java.demo.model.dto.TransactionDto;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("${alias.api.transactions}")
public class TransactionController {

    private final KafkaTransactionProducer kafkaTransactionProducer;

    @PatchMapping("/execute-transaction")
    public ResponseDto executeTransaction(@RequestBody TransactionDto transactionDto, @RequestParam String action) {
        kafkaTransactionProducer.sendTransactionMessage(transactionDto, action);
        return new ResponseDto("Начато выполнение транзакции");
    }
}
