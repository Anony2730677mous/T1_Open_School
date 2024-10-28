package ru.t1.java.demo.service;

import ru.t1.java.demo.model.ClientAccount;

import java.math.BigDecimal;
import java.util.Optional;

public interface ClientAccountService {
    void saveAccount(ClientAccount clientAccount);

    void changeClientAccountType(Long clientAccountId);

    String blockClientAccount(Long clientAccountId);

    boolean checkClientAccountStateById(Long accountId);

    boolean executeTransactionOnClientAccount(Long accountId, BigDecimal executeAmount);

    Optional<ClientAccount> findByClientAccountId(Long accountId);

    String unblockClientAccount(Long clientAccountId);
}
