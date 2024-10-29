package ru.t1.java.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final static String CLIENT_ACCOUNT_FAILURE_MESSAGE = "Счет клиента или клиент не найден";
    private final static String WRONG_CLIENT_ACCOUNT_MESSAGE = "Счет клиента указан неверно";
    private final Integer pageCount = 100;

    @Transactional
    @Override
    public void saveAccount(ClientAccount clientAccount) {
        if (clientAccountRepository.findAccountById(clientAccount.getId()).isPresent()) {
            log.info("Счет с id {} уже существует", clientAccount.getId());
            throw new ClientException("Счет клиента уже существует");
        }
        if (clientAccount.getClient() == null || clientAccount.getClient().getId() == null) {
            log.warn("Счет клиента указан неверно");
            throw new ClientException("Клиент не задан или имеет недопустимый идентификатор");
        }
        Optional<Client> clientOptional = clientRepository.findById(clientAccount.getClient().getId());
        if (clientOptional.isEmpty()) {
            log.warn("Счет клиента не найден");
            throw new ClientException(CLIENT_ACCOUNT_FAILURE_MESSAGE);
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
            log.warn("Счет клиента указан неверно");
            throw new ClientException(WRONG_CLIENT_ACCOUNT_MESSAGE);
        }
        Optional<ClientAccount> accountOptional = clientAccountRepository.findById(clientAccountId);
        if (accountOptional.isEmpty()) {
            log.warn("Счет клиента не найден");
            throw new ClientException(CLIENT_ACCOUNT_FAILURE_MESSAGE);
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

    @Transactional
    @Override
    public String blockClientAccount(Long clientAccountId) {
        if (clientAccountId == null) {
            log.warn("Счет клиента указан неверно");
            throw new ClientException(WRONG_CLIENT_ACCOUNT_MESSAGE);
        }
        Optional<ClientAccount> accountOptional = clientAccountRepository.findById(clientAccountId);
        if (accountOptional.isEmpty()) {
            log.warn("Счет клиента не найден");
            throw new ClientException(CLIENT_ACCOUNT_FAILURE_MESSAGE);
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
            log.warn("Счет клиента указан неверно");
            throw new ClientException(WRONG_CLIENT_ACCOUNT_MESSAGE);
        }
        Optional<ClientAccount> accountOptional = clientAccountRepository.findAccountById(accountId);
        if (accountOptional.isEmpty()) {
            log.warn("Счет клиента не найден");
            throw new ClientException(CLIENT_ACCOUNT_FAILURE_MESSAGE);
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
            throw new ClientException(WRONG_CLIENT_ACCOUNT_MESSAGE);
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

    @Transactional
    @Override
    public String unblockClientAccount(Long clientAccountId) {
        if (clientAccountId == null) {
            throw new ClientException(WRONG_CLIENT_ACCOUNT_MESSAGE);
        }
        Optional<ClientAccount> accountOptional = clientAccountRepository.findById(clientAccountId);
        if (accountOptional.isEmpty()) {
            log.warn("Счет клиента не найден");
            throw new ClientException(CLIENT_ACCOUNT_FAILURE_MESSAGE);
        }

        ClientAccount clientAccount = accountOptional.get();
        String blockedAccountState = "BLOCKED";
        String debitAccountType = "DEBIT";
        String creditAccountType = "CREDIT";
        StringBuilder resultMessage = new StringBuilder("Счет с номером: ");

        if (clientAccount.getClientAccountState().name().equals(blockedAccountState) &&
                clientAccount.getClientAccountType().name().equals(debitAccountType)) {
            clientAccount.setClientAccountState(ClientAccountState.ACTIVE);
            resultMessage.append(clientAccountId).append(" успешно разблокирован");
            clientAccountRepository.save(clientAccount);
            log.info("Счет клиента разблокирован");
        } else if (clientAccount.getClientAccountState().name().equals(blockedAccountState) &&
                clientAccount.getClientAccountType().name().equals(creditAccountType)) {
            BigDecimal creditAccountTypeBalance = clientAccount.getBalance();
            if (creditAccountTypeBalance.signum() > 0) {
                clientAccount.setClientAccountState(ClientAccountState.ACTIVE);
                resultMessage.append(clientAccountId).append(" успешно разблокирован");
                clientAccountRepository.save(clientAccount);
                log.info("Счет клиента разблокирован");
            } else {
                resultMessage.append(clientAccountId).append(" имеет отрицательный баланс и не может быть разблокирован");
            }
        } else {
            resultMessage.append(clientAccountId).append(" уже разблокирован");
        }
        return resultMessage.toString();
    }

    @Transactional
    @Override
    public void blockNegativeBalanceCreditClientAccounts() {
        Pageable pageable = PageRequest.of(0, pageCount);
        Page<Client> clientPage;

        do {
            clientPage = clientRepository.findAll(pageable);
            if (clientPage.isEmpty()) {
                log.info("Список клиентов пуст на странице {}", pageable.getPageNumber());
                break;
            }
            clientPage.getContent().stream()
                    .flatMap(client -> client.getAccounts().stream())
                    .filter(account -> account.getClientAccountType() == ClientAccountType.CREDIT)
                    .filter(account -> account.getClientAccountState() == ClientAccountState.ACTIVE)
                    .filter(account -> account.getBalance().compareTo(BigDecimal.ZERO) < 0)
                    .forEach(account -> {
                        account.setClientAccountState(ClientAccountState.BLOCKED);
                        clientAccountRepository.save(account);
                        log.info("Счет с ID {} заблокирован из-за отрицательного баланса", account.getId());
                    });

            pageable = clientPage.nextPageable();
        } while (clientPage.hasNext());


    }
}
