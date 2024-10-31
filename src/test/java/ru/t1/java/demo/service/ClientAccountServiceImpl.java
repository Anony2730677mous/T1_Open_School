package ru.t1.java.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.t1.java.demo.exception.ClientException;
import ru.t1.java.demo.model.Client;
import ru.t1.java.demo.model.ClientAccount;
import ru.t1.java.demo.model.ClientAccountState;
import ru.t1.java.demo.model.ClientAccountType;
import ru.t1.java.demo.repository.ClientAccountRepository;
import ru.t1.java.demo.repository.ClientRepository;
import ru.t1.java.demo.service.impl.ClientAccountServiceImpl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientAccountServiceImplTest {
    @Mock
    private ClientAccountRepository clientAccountRepository;
    @Mock
    private ClientRepository clientRepository;
    @InjectMocks
    private ClientAccountServiceImpl clientAccountService;
    private ClientAccount clientAccount;
    private Client client;

    @BeforeEach
    public void setUp() {
        client = new Client();
        client.setId(1L);
        client.setFirstName("John");
        client.setLastName("Doe");

        clientAccount = ClientAccount.builder()
                .id(1L)
                .clientAccountType(ClientAccountType.DEBIT)
                .balance(BigDecimal.valueOf(1000))
                .clientAccountState(ClientAccountState.ACTIVE)
                .client(client)
                .build();

        client.addClientAccount(clientAccount);
    }

    @Test
    public void testSaveAccount_ClientAccountAlreadyExists() {
        when(clientAccountRepository.findAccountById(clientAccount.getId())).thenReturn(Optional.of(clientAccount));
        Exception exception = assertThrows(ClientException.class, () -> {
            clientAccountService.saveAccount(clientAccount);
        });
        String expectedMessage = "Счет клиента уже существует";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(clientAccountRepository, times(1)).findAccountById(clientAccount.getId());
        verify(clientAccountRepository, never()).save(any(ClientAccount.class));
    }


    @Test
    public void testSaveAccount_ClientNotSet() {
        clientAccount.setClient(null);

        Exception exception = assertThrows(ClientException.class, () -> {
            clientAccountService.saveAccount(clientAccount);
        });

        String expectedMessage = "Клиент не задан или имеет недопустимый идентификатор";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(clientAccountRepository, never()).save(any(ClientAccount.class));
    }

    @Test
    public void testSaveAccount_ClientNotFound() {
        when(clientRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(ClientException.class, () -> {
            clientAccountService.saveAccount(clientAccount);
        });

        String expectedMessage = "Счет клиента или клиент не найден";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(clientAccountRepository, never()).save(any(ClientAccount.class));
    }

    @Test
    public void testSaveAccount_Success() {
        when(clientAccountRepository.findAccountById(any(Long.class))).thenReturn(Optional.empty());
        when(clientRepository.findById(clientAccount.getClient().getId())).thenReturn(Optional.of(client));
        when(clientAccountRepository.save(any(ClientAccount.class))).thenReturn(clientAccount);

        clientAccountService.saveAccount(clientAccount);

        verify(clientAccountRepository, times(1)).findAccountById(any(Long.class));
        verify(clientRepository, times(1)).findById(clientAccount.getClient().getId());
        verify(clientAccountRepository, times(1)).save(any(ClientAccount.class));

        assertNotNull(clientAccount.getId());
        assertEquals(ClientAccountType.DEBIT, clientAccount.getClientAccountType());
        assertEquals(BigDecimal.valueOf(1000), clientAccount.getBalance());
        assertEquals(ClientAccountState.ACTIVE, clientAccount.getClientAccountState());
        assertEquals(client, clientAccount.getClient());
    }


    @Test
    public void testChangeClientAccountType_SuccessDebitToCredit() {
        clientAccount.setClientAccountType(ClientAccountType.DEBIT);
        when(clientAccountRepository.findAccountByIdForUpdate(clientAccount.getId())).thenReturn(Optional.of(clientAccount));

        clientAccountService.changeClientAccountType(clientAccount.getId());

        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(clientAccount.getId());
        verify(clientAccountRepository, times(1)).save(clientAccount);
        assertEquals(ClientAccountType.CREDIT, clientAccount.getClientAccountType());
    }

    @Test
    public void testChangeClientAccountType_SuccessCreditToDebit() {
        clientAccount.setClientAccountType(ClientAccountType.CREDIT);
        when(clientAccountRepository.findAccountByIdForUpdate(clientAccount.getId())).thenReturn(Optional.of(clientAccount));

        clientAccountService.changeClientAccountType(clientAccount.getId());

        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(clientAccount.getId());
        verify(clientAccountRepository, times(1)).save(clientAccount);
        assertEquals(ClientAccountType.DEBIT, clientAccount.getClientAccountType());

    }

    @Test
    public void testChangeClientAccountType_ClientAccountNotFound() {
        when(clientAccountRepository.findAccountByIdForUpdate(any(Long.class))).thenReturn(Optional.empty());

        Exception exception = assertThrows(ClientException.class, () -> {
            clientAccountService.changeClientAccountType(clientAccount.getId());
        });

        String expectedMessage = "Счет клиента или клиент не найден";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(clientAccount.getId());
        verify(clientAccountRepository, never()).save(any(ClientAccount.class));
    }

    @Test
    public void testBlockClientAccount_Success() {
        ClientAccount activeClientAccount = ClientAccount.builder()
                .id(1L)
                .clientAccountType(ClientAccountType.DEBIT)
                .balance(BigDecimal.valueOf(1000))
                .clientAccountState(ClientAccountState.ACTIVE)
                .client(client)
                .build();

        when(clientAccountRepository.findAccountByIdForUpdate(activeClientAccount.getId())).thenReturn(Optional.of(activeClientAccount));

        String result = clientAccountService.blockClientAccount(activeClientAccount.getId());

        assertEquals("Счет с номером: 1 успешно заблокирован", result);
        assertEquals(ClientAccountState.BLOCKED, activeClientAccount.getClientAccountState());
        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(activeClientAccount.getId());
        verify(clientAccountRepository, times(1)).save(activeClientAccount);
    }

    @Test
    public void testBlockClientAccount_AlreadyBlocked() {
        ClientAccount blockedClientAccount = ClientAccount.builder()
                .id(2L)
                .clientAccountType(ClientAccountType.DEBIT)
                .balance(BigDecimal.valueOf(1000))
                .clientAccountState(ClientAccountState.BLOCKED)
                .client(client)
                .build();

        when(clientAccountRepository.findAccountByIdForUpdate(blockedClientAccount.getId())).thenReturn(Optional.of(blockedClientAccount));

        String result = clientAccountService.blockClientAccount(blockedClientAccount.getId());

        assertEquals("Счет с номером: 2 уже заблокирован", result);
        assertEquals(ClientAccountState.BLOCKED, blockedClientAccount.getClientAccountState());
        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(blockedClientAccount.getId());
        verify(clientAccountRepository, never()).save(blockedClientAccount);
    }

    @Test
    public void testBlockClientAccount_InvalidId() {
        Exception exception = assertThrows(ClientException.class, () -> {
            clientAccountService.blockClientAccount(null);
        });

        String expectedMessage = "Счет клиента указан неверно";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(clientAccountRepository, never()).findAccountByIdForUpdate(any(Long.class));
        verify(clientAccountRepository, never()).save(any(ClientAccount.class));
    }

    @Test
    public void testBlockClientAccount_NotFound() {
        when(clientAccountRepository.findAccountByIdForUpdate(any(Long.class))).thenReturn(Optional.empty());

        Exception exception = assertThrows(ClientException.class, () -> {
            clientAccountService.blockClientAccount(1L);
        });

        String expectedMessage = "Счет клиента или клиент не найден";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(any(Long.class));
        verify(clientAccountRepository, never()).save(any(ClientAccount.class));
    }

    @Test
    public void testUnblockClientAccount_SuccessDebitAccount() {
        // Настройка mock объектов для возврата заблокированного дебетового счета
        clientAccount.setClientAccountState(ClientAccountState.BLOCKED);
        clientAccount.setClientAccountType(ClientAccountType.DEBIT);

        when(clientAccountRepository.findAccountByIdForUpdate(clientAccount.getId())).thenReturn(Optional.of(clientAccount));

        String result = clientAccountService.unblockClientAccount(clientAccount.getId());

        assertEquals("Счет с номером: 1 успешно разблокирован", result);
        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(clientAccount.getId());
        verify(clientAccountRepository, times(1)).save(clientAccount);
        assertEquals(ClientAccountState.ACTIVE, clientAccount.getClientAccountState());
    }

    @Test
    public void testUnblockClientAccount_SuccessCreditAccountWithPositiveBalance() {
        // Настройка mock объектов для возврата заблокированного кредитного счета с положительным балансом
        clientAccount.setClientAccountState(ClientAccountState.BLOCKED);
        clientAccount.setClientAccountType(ClientAccountType.CREDIT);
        clientAccount.setBalance(BigDecimal.valueOf(1000));

        when(clientAccountRepository.findAccountByIdForUpdate(clientAccount.getId())).thenReturn(Optional.of(clientAccount));

        String result = clientAccountService.unblockClientAccount(clientAccount.getId());

        assertEquals("Счет с номером: 1 успешно разблокирован", result);
        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(clientAccount.getId());
        verify(clientAccountRepository, times(1)).save(clientAccount);
        assertEquals(ClientAccountState.ACTIVE, clientAccount.getClientAccountState());
    }

    @Test
    public void testUnblockClientAccount_CreditAccountWithNegativeBalance() {
        clientAccount.setClientAccountState(ClientAccountState.BLOCKED);
        clientAccount.setClientAccountType(ClientAccountType.CREDIT);
        clientAccount.setBalance(BigDecimal.valueOf(-1000));

        when(clientAccountRepository.findAccountByIdForUpdate(clientAccount.getId())).thenReturn(Optional.of(clientAccount));

        String result = clientAccountService.unblockClientAccount(clientAccount.getId());

        assertEquals("Счет с номером: 1 имеет отрицательный баланс и не может быть разблокирован", result);
        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(clientAccount.getId());
        verify(clientAccountRepository, never()).save(clientAccount);
        assertNotEquals(ClientAccountState.ACTIVE, clientAccount.getClientAccountState());
    }

    @Test
    public void testUnblockClientAccount_AlreadyActiveAccount() {
        clientAccount.setClientAccountState(ClientAccountState.ACTIVE);

        when(clientAccountRepository.findAccountByIdForUpdate(clientAccount.getId())).thenReturn(Optional.of(clientAccount));

        String result = clientAccountService.unblockClientAccount(clientAccount.getId());

        assertEquals("Счет с номером: 1 уже разблокирован", result);
        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(clientAccount.getId());
        verify(clientAccountRepository, never()).save(clientAccount);
        assertNotEquals(ClientAccountState.BLOCKED, clientAccount.getClientAccountState());
    }

    @Test
    public void testUnblockClientAccount_AccountNotFound() {
        when(clientAccountRepository.findAccountByIdForUpdate(clientAccount.getId())).thenReturn(Optional.empty());

        Exception exception = assertThrows(ClientException.class, () -> {
            clientAccountService.unblockClientAccount(clientAccount.getId());
        });

        String expectedMessage = "Счет клиента или клиент не найден";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(clientAccount.getId());
        verify(clientAccountRepository, never()).save(clientAccount);
    }

    @Test
    public void testExecuteTransactionOnClientAccount_Success_AddAmount() {
        when(clientAccountRepository.findAccountByIdForUpdate(clientAccount.getId())).thenReturn(Optional.of(clientAccount));

        BigDecimal executeAmount = BigDecimal.valueOf(500);
        BigDecimal expectedBalance = clientAccount.getBalance().add(executeAmount);

        boolean result = clientAccountService.executeTransactionOnClientAccount(clientAccount.getId(), executeAmount);

        assertTrue(result);
        assertEquals(expectedBalance, clientAccount.getBalance());
        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(clientAccount.getId());
        verify(clientAccountRepository, times(1)).save(clientAccount);
    }

    @Test
    public void testExecuteTransactionOnClientAccount_Success_SubtractAmount() {
        when(clientAccountRepository.findAccountByIdForUpdate(clientAccount.getId())).thenReturn(Optional.of(clientAccount));

        BigDecimal executeAmount = BigDecimal.valueOf(-500);
        BigDecimal expectedBalance = clientAccount.getBalance().subtract(executeAmount);

        boolean result = clientAccountService.executeTransactionOnClientAccount(clientAccount.getId(), executeAmount);

        assertTrue(result);
        assertEquals(expectedBalance, clientAccount.getBalance());
        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(clientAccount.getId());
        verify(clientAccountRepository, times(1)).save(clientAccount);
    }

    @Test
    public void testExecuteTransactionOnClientAccount_InvalidAccountId() {
        when(clientAccountRepository.findAccountByIdForUpdate(any(Long.class))).thenReturn(Optional.empty());

        BigDecimal executeAmount = BigDecimal.valueOf(500);

        boolean result = clientAccountService.executeTransactionOnClientAccount(2L, executeAmount);

        assertFalse(result);
        verify(clientAccountRepository, times(1)).findAccountByIdForUpdate(any(Long.class));
        verify(clientAccountRepository, never()).save(any(ClientAccount.class));
    }

    @Test
    public void testExecuteTransactionOnClientAccount_AccountIdIsNull() {
        BigDecimal executeAmount = BigDecimal.valueOf(500);

        Exception exception = assertThrows(ClientException.class, () -> {
            clientAccountService.executeTransactionOnClientAccount(null, executeAmount);
        });

        String expectedMessage = "Счет клиента указан неверно";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(clientAccountRepository, never()).findAccountByIdForUpdate(any(Long.class));
        verify(clientAccountRepository, never()).save(any(ClientAccount.class));
    }

    @Test
    public void testCheckClientAccountStateById_Success_Blocked() {
        clientAccount.setClientAccountState(ClientAccountState.BLOCKED);
        when(clientAccountRepository.findAccountById(clientAccount.getId())).thenReturn(Optional.of(clientAccount));

        boolean result = clientAccountService.checkClientAccountStateById(clientAccount.getId());

        assertTrue(result);
        verify(clientAccountRepository, times(1)).findAccountById(clientAccount.getId());
    }

    @Test
    public void testCheckClientAccountStateById_Success_Closed() {
        clientAccount.setClientAccountState(ClientAccountState.CLOSED);
        when(clientAccountRepository.findAccountById(clientAccount.getId())).thenReturn(Optional.of(clientAccount));

        boolean result = clientAccountService.checkClientAccountStateById(clientAccount.getId());

        assertTrue(result);
        verify(clientAccountRepository, times(1)).findAccountById(clientAccount.getId());
    }

    @Test
    public void testCheckClientAccountStateById_Active() {
        clientAccount.setClientAccountState(ClientAccountState.ACTIVE);
        when(clientAccountRepository.findAccountById(clientAccount.getId())).thenReturn(Optional.of(clientAccount));

        boolean result = clientAccountService.checkClientAccountStateById(clientAccount.getId());

        assertFalse(result);
        verify(clientAccountRepository, times(1)).findAccountById(clientAccount.getId());
    }

    @Test
    public void testCheckClientAccountStateById_InvalidAccountId() {
        when(clientAccountRepository.findAccountById(any(Long.class))).thenReturn(Optional.empty());

        Exception exception = assertThrows(ClientException.class, () -> {
            clientAccountService.checkClientAccountStateById(2L);
        });

        String expectedMessage = "Счет клиента или клиент не найден";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(clientAccountRepository, times(1)).findAccountById(any(Long.class));
    }

    @Test
    public void testCheckClientAccountStateById_AccountIdIsNull() {
        Exception exception = assertThrows(ClientException.class, () -> {
            clientAccountService.checkClientAccountStateById(null);
        });

        String expectedMessage = "Счет клиента указан неверно";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(clientAccountRepository, never()).findAccountById(any(Long.class));
    }

    @Test
    public void testBlockNegativeBalanceCreditClientAccounts_Success() {
        ClientAccount negativeBalanceCreditAccount = ClientAccount.builder()
                .id(2L)
                .clientAccountType(ClientAccountType.CREDIT)
                .balance(BigDecimal.valueOf(-100))
                .clientAccountState(ClientAccountState.ACTIVE)
                .build();

        Client clientWithNegativeBalance = new Client();
        clientWithNegativeBalance.setId(2L);
        clientWithNegativeBalance.setFirstName("Jane");
        clientWithNegativeBalance.setLastName("Doe");
        clientWithNegativeBalance.addClientAccount(negativeBalanceCreditAccount);

        Page<Client> clientPage = new PageImpl<>(Collections.singletonList(clientWithNegativeBalance));

        when(clientRepository.findAll(any(Pageable.class))).thenReturn(clientPage);

        clientAccountService.blockNegativeBalanceCreditClientAccounts();

        verify(clientRepository, times(1)).findAll(any(Pageable.class));
        verify(clientAccountRepository, times(1)).save(negativeBalanceCreditAccount);
        assertEquals(ClientAccountState.BLOCKED, negativeBalanceCreditAccount.getClientAccountState());
    }

    @Test
    public void testBlockNegativeBalanceCreditClientAccounts_EmptyPage() {
        Page<Client> emptyClientPage = new PageImpl<>(Collections.emptyList());

        when(clientRepository.findAll(any(Pageable.class))).thenReturn(emptyClientPage);

        clientAccountService.blockNegativeBalanceCreditClientAccounts();

        verify(clientRepository, times(1)).findAll(any(Pageable.class));
        verify(clientAccountRepository, never()).save(any(ClientAccount.class));
    }

    @Test
    public void testBlockNegativeBalanceCreditClientAccounts_NoNegativeBalanceAccounts() {
        ClientAccount positiveBalanceCreditAccount = ClientAccount.builder()
                .id(3L)
                .clientAccountType(ClientAccountType.CREDIT)
                .balance(BigDecimal.valueOf(100))
                .clientAccountState(ClientAccountState.ACTIVE)
                .build();

        Client clientWithPositiveBalance = new Client();
        clientWithPositiveBalance.setId(3L);
        clientWithPositiveBalance.setFirstName("Jake");
        clientWithPositiveBalance.setLastName("Smith");
        clientWithPositiveBalance.addClientAccount(positiveBalanceCreditAccount);

        Page<Client> clientPage = new PageImpl<>(Collections.singletonList(clientWithPositiveBalance));

        when(clientRepository.findAll(any(Pageable.class))).thenReturn(clientPage);

        clientAccountService.blockNegativeBalanceCreditClientAccounts();

        verify(clientRepository, times(1)).findAll(any(Pageable.class));
        verify(clientAccountRepository, never()).save(any(ClientAccount.class));
        assertEquals(ClientAccountState.ACTIVE, clientWithPositiveBalance.getAccounts().get(0).getClientAccountState());
    }


}
