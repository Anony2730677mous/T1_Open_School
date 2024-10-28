package ru.t1.java.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.t1.java.demo.model.Client;
import ru.t1.java.demo.model.ClientAccountState;
import ru.t1.java.demo.model.ClientAccountType;
import ru.t1.java.demo.repository.ClientAccountRepository;
import ru.t1.java.demo.repository.ClientRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {
    private final ClientRepository clientRepository;
    private final ClientAccountRepository clientAccountRepository;
    private final Integer pageCount = 100;

    @Transactional
    public void blockNegativeBalanceCreditClientAccounts() {
        Pageable pageable = PageRequest.of(0, pageCount);
        Page<Client> clientPage;

        do {
            clientPage = clientRepository.findAll(pageable);
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

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduleBlockNegativeBalanceCreditClientAccounts() {
        log.info("Ночной ежедневный запуск планировщика для блокировки кредитных счетов с отрицательным балансом.");
        blockNegativeBalanceCreditClientAccounts();
    }
}
