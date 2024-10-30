package ru.t1.java.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import ru.t1.java.demo.exception.TransactionException;
import ru.t1.java.demo.kafka.KafkaTransactionProducer;
import ru.t1.java.demo.mapper.TransactionMapper;
import ru.t1.java.demo.model.CorrectionTransaction;
import ru.t1.java.demo.model.Transaction;
import ru.t1.java.demo.model.TransactionActionType;
import ru.t1.java.demo.model.TransactionStateType;
import ru.t1.java.demo.repository.CorrectionTransactionRepository;
import ru.t1.java.demo.repository.TransactionRepository;
import ru.t1.java.demo.service.ClientAccountService;
import ru.t1.java.demo.service.TransactionService;
import ru.t1.java.demo.util.JwtUtils;

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
    private final WebClient webClient;
    private final CorrectionTransactionRepository correctionTransactionRepository;
    private final JwtUtils jwtUtils;
    private final static String CLIENT_ACCOUNT_BLOCKED_MESSAGE = "Счет закрыт или заблокирован. Транзакция не может быть сохранена.";
    private final static String TRANSACTION_FAILURE_MESSAGE = "Транзакция не выполнена";
    private final static String CREATE = "CREATE";
    private final static String DELETE = "DELETE";
    private final static String DONE = "DONE";
    private final Integer pageCount = 100;


    @Override
    @Transactional
    public void saveTransaction(Transaction transaction) {
        if (transaction.getTransactionId() == null) {
            log.warn("Транзакция без id");
            throw new TransactionException("Транзакция не может быть сохранена без идентификатора");
        }
        Optional<Transaction> existingTransactionOptional = transactionRepository
                .findByTransactionalId(transaction.getTransactionId());
        if (existingTransactionOptional.isPresent() &&
                DONE.equalsIgnoreCase(String.valueOf(transaction.getTransactionStateType()))) {
            log.info("Транзакция с id {} уже существует и будет пропущена", transaction.getTransactionId());
            throw new TransactionException("Транзакция уже выполнена");
        }

        Long accountId = transaction.getAccountId();
        boolean isBlocked = checkAccountById(accountId);
        if (isBlocked) {
            kafkaTransactionProducer.sendTransactionErrorMessage(transactionMapper.toDto(transaction), CREATE);
            log.warn("Транзакция с id {} направлена в сервис корректировки", transaction.getTransactionId());
            throw new TransactionException(CLIENT_ACCOUNT_BLOCKED_MESSAGE);
        } else {
            BigDecimal executeAmount = transaction.getAmount();
            boolean transactionIsDone = executeTransactionOnAccount(accountId, executeAmount);
            if (transactionIsDone) {
                transaction.setTransactionStateType(TransactionStateType.DONE);
                transactionRepository.save(transaction);
                log.info("Выполнена транзакция с id: " + transaction.getTransactionId());
            } else {
                log.warn("Транзакция с id {} не выполнена", transaction.getTransactionId());
                throw new TransactionException(TRANSACTION_FAILURE_MESSAGE);
            }
        }
    }

    @Override
    @Transactional
    public void deleteTransaction(Transaction transaction) {
        Optional<Transaction> existingTransactionOptional = transactionRepository
                .findByTransactionalId(transaction.getTransactionId());
        if (existingTransactionOptional.isPresent() &&
                DONE.equalsIgnoreCase(String.valueOf(transaction.getTransactionStateType()))) {
            Long accountId = transaction.getAccountId();
            boolean isBlocked = checkAccountById(accountId);
            if (isBlocked) {
                kafkaTransactionProducer.sendTransactionErrorMessage(transactionMapper.toDto(transaction), DELETE);
                log.warn("Транзакция с id {} направлена в сервис корректировки", transaction.getTransactionId());
                throw new TransactionException(CLIENT_ACCOUNT_BLOCKED_MESSAGE);
            }

            BigDecimal deletedAmount = transaction.getAmount();
            BigDecimal invertedAmount;
            /*
            Для отката транзакции инвертируем значение её amount и
            пользуемся существующим методом executeTransactionOnAccount для изменения величины баланса на счете
             */
            if (deletedAmount.signum() > 0) {
                invertedAmount = deletedAmount.negate();
            } else {
                invertedAmount = deletedAmount.abs();
            }
            boolean transactionIsDone = executeTransactionOnAccount(accountId, invertedAmount);

            if (transactionIsDone) {
                transactionRepository.deleteById(Long.valueOf(existingTransactionOptional.get().getTransactionId()));
                log.info("Транзакция с id {} успешно удалена", transaction.getTransactionId());
            } else {
                log.warn("Транзакция с id {} не удалена", transaction.getTransactionId());
                throw new TransactionException(TRANSACTION_FAILURE_MESSAGE);
            }

        } else {
            log.warn("Транзакция с id {} не найдена", transaction.getTransactionId());
            throw new TransactionException("Транзакция не найдена");
        }
    }

    @Transactional
    @Override
    public void correctionTransaction(Transaction transaction, String action) {
        if (transaction.getTransactionId() == null) {
            log.warn("Транзакция без id");
            throw new TransactionException("Транзакция не может быть обработана без идентификатора");
        }

        Optional<CorrectionTransaction> existingCorrrectionTransactionOptional = correctionTransactionRepository
                .findByCorrectionalTransactionalId(transaction.getTransactionId());
        Long accountId = transaction.getAccountId();
        boolean isBlocked = checkAccountById(accountId);
        if (existingCorrrectionTransactionOptional.isPresent()) {
            if (isBlocked) {
                boolean isUnblock = callingClientAccountUnblock(accountId);
                if (isUnblock) {
                    correctionTransactionRepository.deleteById(Long.valueOf(existingCorrrectionTransactionOptional.get().getTransactionId()));
                    log.info("Транзакция с id {} успешно удалена из таблицы correction_transaction", transaction.getTransactionId());
                } else {
                    log.info("Счет после запроса на разблокировку не был разблокирован");
                }
            } else {
                log.info("Счет перед запросом на разблокировку уже был разблокирован");
            }
        } else {
            if (isBlocked) {
                boolean isUnblock = callingClientAccountUnblock(accountId);
                if (isUnblock) {
                    log.info("Счет после запроса на разблокировку был разблокирован");
                } else {
                    CorrectionTransaction correctionTransaction = CorrectionTransaction.builder()
                            .amount(transaction.getAmount())
                            .clientId(transaction.getClientId())
                            .accountId(transaction.getAccountId())
                            .transactionId(transaction.getTransactionId())
                            .transactionActionType(TransactionActionType.valueOf(action))
                            .transactionStateType(transaction.getTransactionStateType())
                            .build();
                    correctionTransactionRepository.save(correctionTransaction);
                    log.info("Транзакция с id {} сохранена в таблицу correction_transaction", transaction.getTransactionId());
                }
            } else {
                log.info("Счет перед запросом на разблокировку уже был разблокирован");
            }
        }

    }

    private boolean callingClientAccountUnblock(Long accountId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.info("Ошибка во время процесса аутентификации в методе callingClientAccountUnblock");
            throw new IllegalStateException("Не удалось получить контекст аутентификации");
        }

        String jwtToken = jwtUtils.generateJwtToken(authentication);
        try {
            String response = webClient.patch()
                    .uri("/api/accounts/unblock-account/{id}", accountId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null && response.contains("разблокирован")) {
                return true;
            } else if (response != null && response.contains("не может быть")) {
                return false;
            } else {
                throw new TransactionException(TRANSACTION_FAILURE_MESSAGE);
            }
        } catch (WebClientResponseException e) {
            log.warn("Ошибка HTTP: " + e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Ошибка при выполнении запроса на разблокировку счета: " + e.getMessage());
            return false;
        }
    }

    private boolean executeTransactionOnAccount(Long accountId, BigDecimal executeAmount) {
        return clientAccountService.executeTransactionOnClientAccount(accountId, executeAmount);
    }

    private boolean checkAccountById(Long accountId) {
        return clientAccountService.checkClientAccountStateById(accountId);
    }

    @Override
    public void processingListOfCorrectionTransactions() {
        Pageable pageable = PageRequest.of(0, pageCount);
        Page<CorrectionTransaction> transactionsPage;

        do {
            transactionsPage = correctionTransactionRepository.findAll(pageable);
            if (transactionsPage.isEmpty()) {
                log.info("Список транзакций пуст на странице {}", pageable.getPageNumber());
                break;
            }
            transactionsPage.getContent().stream()
                    .forEach(transaction -> {
                        kafkaTransactionProducer.
                                sendTransactionMessage(transactionMapper.toDto(transaction), transaction.getTransactionActionType().name());
                        log.info("Транзакция с ID {} отправлена на повторную обработку", transaction.getTransactionId());
                    });

            pageable = transactionsPage.nextPageable();
        } while (transactionsPage.hasNext());
    }
}
