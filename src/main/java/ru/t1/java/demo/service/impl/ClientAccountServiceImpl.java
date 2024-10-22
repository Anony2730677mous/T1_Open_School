package ru.t1.java.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.t1.java.demo.exception.ClientException;
import ru.t1.java.demo.model.Client;
import ru.t1.java.demo.model.ClientAccount;
import ru.t1.java.demo.repository.ClientAccountRepository;
import ru.t1.java.demo.repository.ClientRepository;
import ru.t1.java.demo.service.ClientAccountService;

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
}
