package ru.t1.java.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.t1.java.demo.exception.TransactionException;
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
        if (transaction.getTransactionId() == null) {
            throw new TransactionException("Транзакция не может быть сохранена без идентификатора");
        }
        log.info("Проверка существования транзакции с id: {}", transaction.getTransactionId());
        if (transactionRepository.findByTransactionalId(transaction.getTransactionId()).isPresent()) {
            log.info("Транзакция с id {} уже существует и будет пропущена", transaction.getTransactionId());
            throw new TransactionException("Транзакция уже выполнена");
        }
        transactionRepository.save(transaction);
        log.info("Выполнена транзакция с id: " + transaction.getTransactionId());
    }
}
