package ru.t1.java.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.t1.java.demo.exception.ClientException;
import ru.t1.java.demo.model.Client;
import ru.t1.java.demo.model.ClientAccount;
import ru.t1.java.demo.model.ClientAccountState;
import ru.t1.java.demo.model.ClientAccountType;
import ru.t1.java.demo.repository.ClientAccountRepository;
import ru.t1.java.demo.repository.ClientRepository;
import ru.t1.java.demo.service.ClientAccountService;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClientAccountServiceImpl implements ClientAccountService {
    private final ClientAccountRepository clientAccountRepository;
    private final ClientRepository clientRepository;

    @Transactional
    @Override
    public void saveAccount(ClientAccount clientAccount) {
        if (clientAccountRepository.findAccountById(clientAccount.getId()).isPresent()) {
            log.info("Счет с id {} уже существует", clientAccount.getId());
            throw new ClientException("Счет клиента уже существует");
        }
        if (clientAccount.getClient() == null || clientAccount.getClient().getId() == null) {
            throw new ClientException("Клиент не задан или имеет недопустимый идентификатор");
        }
        Optional<Client> clientOptional = clientRepository.findById(clientAccount.getClient().getId());
        if (clientOptional.isEmpty()) {
            throw new ClientException("Клиент не найден");
        }
        Client client = clientOptional.get();
        client.addClientAccount(clientAccount);
        clientAccountRepository.save(clientAccount);
        log.info("Счет с id {} сохранен успешно для клиента с id {}", clientAccount.getId(), client.getId());
    }

    @Transactional
    @Override
    public void changeClientAccountType(Long clientAccountId) {
        if (clientAccountId == null) {
            throw new ClientException("Счет клиента указан неверно");
        }
        Optional<ClientAccount> accountOptional = clientAccountRepository.findById(clientAccountId);
        if (accountOptional.isEmpty()) {
            log.warn("Счет клиента не найден");
            throw new ClientException("Счет клиента не найден");
        }
        ClientAccount clientAccount = accountOptional.get();

        String accountType = "CREDIT";
        if (clientAccount.getClientAccountType().name().equals(accountType)) {
            clientAccount.setClientAccountType(ClientAccountType.DEBIT);
        } else {
            clientAccount.setClientAccountType(ClientAccountType.CREDIT);
        }
        clientAccountRepository.save(clientAccount);
        log.info("Тип счета изменен");
    }

    @Override
    public String blockClientAccount(Long clientAccountId) {
        if (clientAccountId == null) {
            throw new ClientException("Счет клиента указан неверно");
        }
        Optional<ClientAccount> accountOptional = clientAccountRepository.findById(clientAccountId);
        if (accountOptional.isEmpty()) {
            log.warn("Счет клиента не найден");
            throw new ClientException("Счет клиента не найден");
        }
        ClientAccount clientAccount = accountOptional.get();
        String accountState = "ACTIVE";
        StringBuilder resultMessage = new StringBuilder("Счет с номером: ");
        if (clientAccount.getClientAccountState().name().equals(accountState)) {
            clientAccount.setClientAccountState(ClientAccountState.BLOCKED);
            resultMessage.append(clientAccountId).append(" успешно заблокирован");
            clientAccountRepository.save(clientAccount);
            log.info("Счет клиента заблокирован");
        } else {
            resultMessage.append(clientAccountId).append(" уже заблокирован");
        }
        return resultMessage.toString();
    }

    @Override
    public boolean checkClientAccountStateById(Long accountId) {
        if (accountId == null) {
            throw new ClientException("Счет клиента указан неверно");
        }
        Optional<ClientAccount> accountOptional = clientAccountRepository.findAccountById(accountId);
        if (accountOptional.isEmpty()) {
            log.warn("Счет клиента не найден");
            throw new ClientException("Счет клиента не найден");
        }
        ClientAccount clientAccount = accountOptional.get();
        String blocked = "BLOCKED";
        String closed = "CLOSED";
        return clientAccount.getClientAccountState().name().equals(blocked) ||
                clientAccount.getClientAccountState().name().equals(closed);
    }

    @Transactional
    @Override
    public boolean executeTransactionOnClientAccount(Long accountId, BigDecimal executeAmount) {
        if (accountId == null) {
            log.warn("Счет клиента указан неверно");
            throw new ClientException("Счет клиента указан неверно");
        }
        Optional<ClientAccount> accountOptional = clientAccountRepository.findAccountById(accountId);
        if (accountOptional.isEmpty()) {
            log.warn("Счет клиента не найден");
            return false;
        } else {
            ClientAccount clientAccount = accountOptional.get();
            BigDecimal clientAccountBalance = clientAccount.getBalance();
            BigDecimal newClientAccountBalance;

            if (executeAmount.signum() < 0) {
                newClientAccountBalance = clientAccountBalance.subtract(executeAmount);
            } else {
                newClientAccountBalance = clientAccountBalance.add(executeAmount);
            }
            clientAccount.setBalance(newClientAccountBalance);
            clientAccountRepository.save(clientAccount);
            log.info("Баланс счета c номером: {} был успешно изменен", clientAccount.getId());
            return true;
        }


    }

    @Override
    public Optional<ClientAccount> findByClientAccountId(Long accountId) {
        return clientAccountRepository.findAccountById(accountId);

    }
}
