package ru.t1.java.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.t1.java.demo.model.ClientAccount;
import ru.t1.java.demo.repository.ClientAccountRepository;
import ru.t1.java.demo.service.ClientAccountService;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClientAccountServiceImpl implements ClientAccountService {
    private final ClientAccountRepository clientAccountRepository;

    @Transactional
    @Override
    public void saveAccount(ClientAccount clientAccount) {
        if (clientAccountRepository.findAccountById(clientAccount.getId()).isPresent()){
            log.info("Счет с id {} уже существует", clientAccount.getId());
            return;
        }
        clientAccountRepository.save(clientAccount);
        log.info("Счет с id {} сохранен успешно", clientAccount.getId());
    }
}
