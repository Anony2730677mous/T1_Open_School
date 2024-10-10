package ru.t1.java.demo.service;

import com.github.javafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.t1.java.demo.model.Client;
import ru.t1.java.demo.model.ClientAccount;
import ru.t1.java.demo.model.ClientAccountType;
import ru.t1.java.demo.model.DataSourceErrorLog;
import ru.t1.java.demo.model.TimeLimitExceedLog;
import ru.t1.java.demo.repository.ClientAccountRepository;
import ru.t1.java.demo.repository.ClientRepository;
import ru.t1.java.demo.repository.DataSourceErrorLogRepository;
import ru.t1.java.demo.repository.TimeLimitExceedLogRepository;

import java.math.BigDecimal;
import java.util.stream.IntStream;

@Service
public class TestDataLoaderService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientAccountRepository clientAccountRepository;

    @Autowired
    private DataSourceErrorLogRepository dataSourceErrorLogRepository;

    @Autowired
    private TimeLimitExceedLogRepository timeLimitExceedLogRepository;

    private Faker faker = new Faker();

    @Transactional
    public void loadData() {
        IntStream.range(0, 100).forEach(i -> {
            Client client = new Client();
            client.setFirstName(faker.name().firstName());
            client.setLastName(faker.name().lastName());
            client.setMiddleName(faker.name().nameWithMiddle());

            client = clientRepository.save(client);

            ClientAccount clientAccount = new ClientAccount();
            clientAccount.setClientAccountType(ClientAccountType.DEBIT);
            clientAccount.setBalance(BigDecimal.valueOf(faker.number().randomDouble(2, 100, 10000)));
            client.addClientAccount(clientAccount);

            DataSourceErrorLog errorLog = new DataSourceErrorLog();
            errorLog.setStackTrace(faker.lorem().characters(250));
            errorLog.setMessage(faker.lorem().sentence());
            errorLog.setMethodSignature(faker.lorem().word());

            dataSourceErrorLogRepository.save(errorLog);

            TimeLimitExceedLog timeLimitLog = new TimeLimitExceedLog();
            timeLimitLog.setMethodSignature(faker.lorem().word());
            timeLimitLog.setExecutionTime(faker.number().numberBetween(1000L, 10000L));

            timeLimitExceedLogRepository.save(timeLimitLog);
        });
    }
}

