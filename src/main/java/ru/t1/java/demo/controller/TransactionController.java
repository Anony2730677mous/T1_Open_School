package ru.t1.java.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.t1.java.demo.kafka.KafkaTransactionProducer;
import ru.t1.java.demo.model.dto.TransactionDto;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("${alias.api.transactions}")
@PreAuthorize("hasRole('USER')")
public class TransactionController {

    private final KafkaTransactionProducer kafkaTransactionProducer;

    @PatchMapping("/execute-transaction")
    public ResponseDto executeTransaction(@RequestBody TransactionDto transactionDto, @RequestParam String action) {
        kafkaTransactionProducer.sendTransactionMessage(transactionDto, action);
        log.info("Выполняется транзакция с номером {}", transactionDto.getTransactionId());
        return new ResponseDto("Начато выполнение транзакции");
    }
}
