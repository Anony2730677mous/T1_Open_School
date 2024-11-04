package ru.t1.java.demo.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.t1.java.demo.mapper.TransactionMapper;
import ru.t1.java.demo.model.Transaction;
import ru.t1.java.demo.model.TransactionActionType;
import ru.t1.java.demo.model.TransactionStateType;
import ru.t1.java.demo.model.dto.CheckResponse;
import ru.t1.java.demo.model.dto.TransactionDto;
import ru.t1.java.demo.service.impl.TransactionServiceImpl;
import ru.t1.java.demo.web.CheckWebClient;

import java.math.BigDecimal;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@SpringBootTest
public class TransactionServiceImplIntegrationTest {

    @Mock
    private CheckWebClient checkWebClient;
    @InjectMocks
    private TransactionServiceImpl transactionServiceImpl;
    @Autowired
    private TransactionMapper transactionMapper;
    private Transaction transaction;
    private TransactionDto transactionDto;

    @BeforeEach
    void setUp() {
        transaction = Transaction.builder()
                .amount(new BigDecimal("100.00"))
                .clientId(1L)
                .accountId(1L)
                .transactionId("12345")
                .transactionActionType(TransactionActionType.CREATE)
                .transactionStateType(TransactionStateType.ACTIVE)
                .build();

        transactionDto = transactionMapper.toDto(transaction);

        stubFor(post(urlEqualTo("/check"))
                .withRequestBody(equalToJson("{ \"clientId\": 1 }"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"blocked\": false }")));
    }

    @Test
    public void testPermissionToMakeTransactionAllowed() {
        when(checkWebClient.check(transaction.getClientId())).thenReturn(Optional.of(new CheckResponse(false)));

        boolean isAllowed = transactionServiceImpl.permissionToMakeTransaction(transaction);

        assertTrue(isAllowed, "Транзакция должна быть разрешена");
    }

    @Test
    public void testPermissionToMakeTransactionBlocked() {
        when(checkWebClient.check(transaction.getClientId())).thenReturn(Optional.of(new CheckResponse(true)));

        boolean isAllowed = transactionServiceImpl.permissionToMakeTransaction(transaction);

        assertFalse(isAllowed, "Транзакция должна быть запрещена");
    }

    @Test
    public void testPermissionToMakeTransactionError() {
        when(checkWebClient.check(transaction.getClientId())).thenReturn(Optional.empty());

        boolean isAllowed = transactionServiceImpl.permissionToMakeTransaction(transaction);

        assertFalse(isAllowed, "Транзакция должна быть запрещена при ошибке");
    }
}



