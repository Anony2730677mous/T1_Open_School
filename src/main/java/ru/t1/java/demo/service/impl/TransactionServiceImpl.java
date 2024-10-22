package ru.t1.java.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.t1.java.demo.model.Transaction;
import ru.t1.java.demo.repository.TransactionRepository;
import ru.t1.java.demo.service.TransactionService;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public void saveTransaction(Transaction transaction) {
        log.info("Проверка существования транзакции с id: {}", transaction.getTransactionId());
        if(transactionRepository.findByTransactionalId(transaction.getTransactionId()).isPresent()){
            log.info("Транзакция с id {} уже существует и будет пропущена", transaction.getTransactionId());
            return;
        }
        log.info("Выполняется транзакция с id: " + transaction.getTransactionId());
        transactionRepository.save(transaction);
    }
}
