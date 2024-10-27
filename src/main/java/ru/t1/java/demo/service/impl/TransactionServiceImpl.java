package ru.t1.java.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.t1.java.demo.exception.TransactionException;
import ru.t1.java.demo.kafka.KafkaTransactionProducer;
import ru.t1.java.demo.mapper.TransactionMapper;
import ru.t1.java.demo.model.Transaction;
import ru.t1.java.demo.repository.TransactionRepository;
import ru.t1.java.demo.service.ClientAccountService;
import ru.t1.java.demo.service.TransactionService;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final ClientAccountService clientAccountService;
    private final KafkaTransactionProducer kafkaTransactionProducer;
    private final TransactionMapper transactionMapper;
    private final static String CLIENT_ACCOUNT_BLOCKED_MESSAGE = "Счет закрыт или заблокирован. Транзакция не может быть сохранена.";
    private final static String CLIENT_ACCOUNT_FAILURE_MESSAGE = "Транзакция не выполнена";
    @Override
    @Transactional
    public void saveTransaction(Transaction transaction) {
        if (transaction.getTransactionId() == null) {
            throw new TransactionException("Транзакция не может быть сохранена без идентификатора");
        }
        if (transactionRepository.findByTransactionalId(transaction.getTransactionId()).isPresent()) {
            log.info("Транзакция с id {} уже существует и будет пропущена", transaction.getTransactionId());
            throw new TransactionException("Транзакция уже выполнена");
        }

        Long accountId = transaction.getAccountId();
        boolean isBlocked = checkAccountById(accountId);
        if (isBlocked) {
            kafkaTransactionProducer.sendTransactionErrorMessage(transactionMapper.toDto(transaction));
            throw new TransactionException(CLIENT_ACCOUNT_BLOCKED_MESSAGE);
        } else {
            BigDecimal executeAmount = transaction.getAmount();
            boolean transactionIsDone = executeTransactionOnAccount(accountId, executeAmount);
            if (transactionIsDone) {
                transactionRepository.save(transaction);
                log.info("Выполнена транзакция с id: " + transaction.getTransactionId());
            } else {
                log.warn("Транзакция с id {} не выполнена", transaction.getTransactionId());
                throw new TransactionException(CLIENT_ACCOUNT_FAILURE_MESSAGE);
            }
        }
    }

    @Override
    @Transactional
    public void deleteTransaction(Transaction transaction) {
        Optional<Transaction> existingTransactionOptional = transactionRepository
                .findByTransactionalId(transaction.getTransactionId());
        if (existingTransactionOptional.isPresent()) {
            Long accountId = transaction.getAccountId();
            boolean isBlocked = checkAccountById(accountId);
            if (isBlocked) {
                kafkaTransactionProducer.sendTransactionErrorMessage(transactionMapper.toDto(transaction));
                throw new TransactionException(CLIENT_ACCOUNT_BLOCKED_MESSAGE);
            }

            BigDecimal deletedAmount = transaction.getAmount();
            BigDecimal invertedAmount;
            /*
            Для отката транзакции инвертируем значение её amount и
            пользуемся существующим методом executeTransactionOnAccount для изменения величины баланса на счете
             */

            if (deletedAmount.signum() > 0) {
                // Если сумма положительная, делаем её отрицательной
                invertedAmount = deletedAmount.negate();
            } else {
                // Если сумма отрицательная, делаем её положительной
                invertedAmount = deletedAmount.abs();
            }
            boolean transactionIsDone = executeTransactionOnAccount(accountId, invertedAmount);

            if (transactionIsDone) {
                transactionRepository.deleteById(Long.valueOf(existingTransactionOptional.get().getTransactionId()));
                log.info("Транзакция с id {} успешно удалена", transaction.getTransactionId());
            } else {
                log.warn("Транзакция с id {} не удалена", transaction.getTransactionId());
                throw new TransactionException(CLIENT_ACCOUNT_FAILURE_MESSAGE);
            }

        } else {
            log.warn("Транзакция с id {} не найдена", transaction.getTransactionId());
            throw new TransactionException("Транзакция не найдена");
        }
    }

    private boolean executeTransactionOnAccount(Long accountId, BigDecimal executeAmount) {
        return clientAccountService.executeTransactionOnClientAccount(accountId, executeAmount);
    }

    private boolean checkAccountById(Long accountId) {
        return clientAccountService.checkClientAccountStateById(accountId);
    }
}
