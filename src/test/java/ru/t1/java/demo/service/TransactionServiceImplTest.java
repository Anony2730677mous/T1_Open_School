package ru.t1.java.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.t1.java.demo.exception.TransactionException;
import ru.t1.java.demo.kafka.KafkaTransactionProducer;
import ru.t1.java.demo.mapper.TransactionMapper;
import ru.t1.java.demo.model.*;
import ru.t1.java.demo.model.dto.TransactionDto;
import ru.t1.java.demo.repository.CorrectionTransactionRepository;
import ru.t1.java.demo.repository.TransactionRepository;
import ru.t1.java.demo.service.impl.TransactionServiceImpl;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ClientAccountService clientAccountService;
    @Mock
    private KafkaTransactionProducer kafkaTransactionProducer;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private CorrectionTransactionRepository correctionTransactionRepository;
    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Transaction transaction;
    private Transaction doneTransaction;

    private ClientAccount clientAccount;

    @BeforeEach
    public void setUp() {
        transaction = new Transaction();
        transaction.setTransactionId("12345");
        transaction.setAccountId(1L);
        transaction.setAmount(BigDecimal.valueOf(1000));
        transaction.setTransactionStateType(TransactionStateType.ACTIVE);

        doneTransaction = new Transaction();
        doneTransaction.setTransactionId("67890");
        doneTransaction.setAccountId(2L);
        doneTransaction.setAmount(BigDecimal.valueOf(1500));
        doneTransaction.setTransactionStateType(TransactionStateType.DONE);

        clientAccount = new ClientAccount();
        clientAccount.setClientAccountState(ClientAccountState.BLOCKED);
    }

    @Test
    public void testSaveTransaction_Success() {
        when(transactionRepository.findByTransactionalId(transaction.getTransactionId())).thenReturn(Optional.empty());
        when(clientAccountService.checkClientAccountStateById(transaction.getAccountId())).thenReturn(false);
        when(clientAccountService.executeTransactionOnClientAccount(transaction.getAccountId(), transaction.getAmount())).thenReturn(true);

        transactionService.saveTransaction(transaction);

        verify(transactionRepository, times(1)).findByTransactionalId(transaction.getTransactionId());
        verify(transactionRepository, times(1)).save(transaction);
        verify(clientAccountService, times(1)).checkClientAccountStateById(transaction.getAccountId());
        verify(clientAccountService, times(1)).executeTransactionOnClientAccount(transaction.getAccountId(), transaction.getAmount());
        assertEquals(TransactionStateType.DONE, transaction.getTransactionStateType());
    }

    @Test
    public void testSaveTransaction_TransactionIdIsNull() {
        transaction.setTransactionId(null);

        Exception exception = assertThrows(TransactionException.class, () -> {
            transactionService.saveTransaction(transaction);
        });

        assertEquals("Транзакция не может быть сохранена без идентификатора", exception.getMessage());
        verify(transactionRepository, never()).findByTransactionalId(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(clientAccountService, never()).checkClientAccountStateById(anyLong());
        verify(clientAccountService, never()).executeTransactionOnClientAccount(anyLong(), any(BigDecimal.class));
        assertNotEquals(TransactionStateType.DONE, transaction.getTransactionStateType());
    }

    @Test
    public void testSaveTransaction_TransactionAlreadyDone() {
        when(transactionRepository.findByTransactionalId(transaction.getTransactionId())).thenReturn(Optional.of(transaction));
        transaction.setTransactionStateType(TransactionStateType.DONE);

        Exception exception = assertThrows(TransactionException.class, () -> {
            transactionService.saveTransaction(transaction);
        });

        assertEquals("Транзакция уже выполнена", exception.getMessage());
        verify(transactionRepository, times(1)).findByTransactionalId(transaction.getTransactionId());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(clientAccountService, never()).checkClientAccountStateById(anyLong());
        verify(clientAccountService, never()).executeTransactionOnClientAccount(anyLong(), any(BigDecimal.class));
    }

    @Test
    public void testSaveTransaction_ClientAccountBlocked() {
        transaction.setTransactionActionType(TransactionActionType.CREATE);
        when(transactionRepository.findByTransactionalId(transaction.getTransactionId())).thenReturn(Optional.empty());
        when(clientAccountService.checkClientAccountStateById(transaction.getAccountId())).thenReturn(true);
        when(transactionMapper.toDto(transaction)).thenReturn(new TransactionDto());

        Exception exception = assertThrows(TransactionException.class, () -> {
            transactionService.saveTransaction(transaction);
        });

        assertEquals("Счет закрыт или заблокирован. Транзакция не может быть сохранена.", exception.getMessage());
        verify(transactionRepository, times(1)).findByTransactionalId(transaction.getTransactionId());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(clientAccountService, times(1)).checkClientAccountStateById(transaction.getAccountId());
        verify(kafkaTransactionProducer, times(1)).sendTransactionErrorMessage(transactionMapper.toDto(transaction), transaction.getTransactionActionType().name());
        verify(clientAccountService, never()).executeTransactionOnClientAccount(anyLong(), any(BigDecimal.class));
        assertNotEquals(TransactionStateType.DONE, transaction.getTransactionStateType());
    }

    @Test
    void testSaveTransaction_TransactionNotExecuted() {
        when(transactionRepository.findByTransactionalId(transaction.getTransactionId())).thenReturn(Optional.empty());
        when(clientAccountService.checkClientAccountStateById(transaction.getAccountId())).thenReturn(false);
        when(clientAccountService.executeTransactionOnClientAccount(transaction.getAccountId(), transaction.getAmount())).thenReturn(false);

        Exception exception = assertThrows(TransactionException.class, () -> {
            transactionService.saveTransaction(transaction);
        });

        String expectedMessage = "Транзакция не выполнена";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(transactionRepository, times(1)).findByTransactionalId(transaction.getTransactionId());
        verify(transactionRepository, never()).save(transaction);
        assertNotEquals(TransactionStateType.DONE, transaction.getTransactionStateType());
    }

    @Test
    public void testDeleteTransaction_Success() {
        when(transactionRepository.findTransactionalByIdForUpdate(doneTransaction.getTransactionId()))
                .thenReturn(Optional.of(doneTransaction));
        when(clientAccountService.checkClientAccountStateById(doneTransaction.getAccountId()))
                .thenReturn(false);
        when(clientAccountService.executeTransactionOnClientAccount(doneTransaction.getAccountId(), doneTransaction.getAmount().negate()))
                .thenReturn(true);

        transactionService.deleteTransaction(doneTransaction);

        verify(transactionRepository, times(1)).findTransactionalByIdForUpdate(doneTransaction.getTransactionId());
        verify(clientAccountService, times(1)).checkClientAccountStateById(doneTransaction.getAccountId());
        verify(clientAccountService, times(1)).executeTransactionOnClientAccount(doneTransaction.getAccountId(), doneTransaction.getAmount().negate());
        verify(transactionRepository, times(1)).deleteById(Long.valueOf(doneTransaction.getTransactionId()));
    }

    @Test
    public void testDeleteTransaction_TransactionNotFound() {
        when(transactionRepository.findTransactionalByIdForUpdate(transaction.getTransactionId()))
                .thenReturn(Optional.empty());

        Exception exception = assertThrows(TransactionException.class, () -> {
            transactionService.deleteTransaction(transaction);
        });

        String expectedMessage = "Транзакция не найдена";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(transactionRepository, times(1)).findTransactionalByIdForUpdate(transaction.getTransactionId());
        verify(clientAccountService, never()).checkClientAccountStateById(any(Long.class));
        verify(transactionRepository, never()).deleteById(any(Long.class));
    }


    @Test
    public void testDeleteTransaction_AccountIsBlocked() {

        transaction.setTransactionStateType(TransactionStateType.DONE);
        transaction.setTransactionActionType(TransactionActionType.DELETE); // Установим тип действия транзакции

        when(transactionRepository.findTransactionalByIdForUpdate(transaction.getTransactionId()))
                .thenReturn(Optional.of(transaction));
        when(clientAccountService.checkClientAccountStateById(transaction.getAccountId()))
                .thenReturn(true);
        when(transactionMapper.toDto(transaction)).thenReturn(new TransactionDto()); // Мокаем вызов toDto

        Exception exception = assertThrows(TransactionException.class, () -> {
            transactionService.deleteTransaction(transaction);
        });

        String expectedMessage = "Счет закрыт или заблокирован. Транзакция не может быть сохранена.";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(kafkaTransactionProducer, times(1)).sendTransactionErrorMessage(transactionMapper.toDto(transaction), transaction.getTransactionActionType().name());
        verify(transactionRepository, never()).deleteById(any(Long.class));

    }

    @Test
    public void testDeleteTransaction_TransactionNotExecuted() {
        when(transactionRepository.findTransactionalByIdForUpdate(doneTransaction.getTransactionId()))
                .thenReturn(Optional.of(doneTransaction));
        when(clientAccountService.checkClientAccountStateById(doneTransaction.getAccountId()))
                .thenReturn(false);
        when(clientAccountService.executeTransactionOnClientAccount(doneTransaction.getAccountId(), doneTransaction.getAmount().negate()))
                .thenReturn(false);

        Exception exception = assertThrows(TransactionException.class, () -> {
            transactionService.deleteTransaction(doneTransaction);
        });

        String expectedMessage = "Транзакция не выполнена";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(transactionRepository, times(1)).findTransactionalByIdForUpdate(doneTransaction.getTransactionId());
        verify(clientAccountService, times(1)).checkClientAccountStateById(doneTransaction.getAccountId());
        verify(clientAccountService, times(1)).executeTransactionOnClientAccount(doneTransaction.getAccountId(), doneTransaction.getAmount().negate());
        verify(transactionRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void testCorrectionTransaction_TransactionIdIsNull() {
        transaction.setTransactionId(null);

        Exception exception = assertThrows(TransactionException.class, () -> {
            transactionService.correctionTransaction(transaction, TransactionActionType.CREATE.name());
        });

        String expectedMessage = "Транзакция не может быть обработана без идентификатора";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        verify(correctionTransactionRepository, never()).findByCorrectionalTransactionalId(anyString());
        verify(correctionTransactionRepository, never()).save(any(CorrectionTransaction.class));
        verify(correctionTransactionRepository, never()).deleteById(anyLong());
    }

    @Test
    void testCorrectionTransaction_NewCorrectionTransaction_AccountNotBlocked() {
        when(correctionTransactionRepository.findByCorrectionalTransactionalId(transaction.getTransactionId()))
                .thenReturn(Optional.empty());
        when(clientAccountService.checkClientAccountStateById(transaction.getAccountId())).thenReturn(false);

        transactionService.correctionTransaction(transaction, TransactionActionType.CREATE.name());

        verify(correctionTransactionRepository, times(1)).findByCorrectionalTransactionalId(transaction.getTransactionId());
        verify(correctionTransactionRepository, never()).save(any(CorrectionTransaction.class));
        verify(clientAccountService, times(1)).checkClientAccountStateById(transaction.getAccountId());
    }


    @Test
    void testCorrectionTransaction_NewCorrectionTransaction_AccountIsBlocked_AccountNotUnblocked() {
        when(correctionTransactionRepository.findByCorrectionalTransactionalId(transaction.getTransactionId()))
                .thenReturn(Optional.empty());
        when(clientAccountService.checkClientAccountStateById(transaction.getAccountId())).thenReturn(true);

        TransactionServiceImpl spyService = Mockito.spy(transactionService);
        doReturn(false).when(spyService).callingClientAccountUnblock(transaction.getAccountId());

        spyService.correctionTransaction(transaction, TransactionActionType.CREATE.name());

        verify(correctionTransactionRepository, times(1)).findByCorrectionalTransactionalId(transaction.getTransactionId());
        verify(correctionTransactionRepository, times(1)).save(any(CorrectionTransaction.class));
        verify(clientAccountService, times(1)).checkClientAccountStateById(transaction.getAccountId());
    }

    @Test
    void testCorrectionTransaction_NewCorrectionTransaction_AccountIsBlocked_AccountUnblocked() {
        when(correctionTransactionRepository.findByCorrectionalTransactionalId(transaction.getTransactionId()))
                .thenReturn(Optional.empty());
        when(clientAccountService.checkClientAccountStateById(transaction.getAccountId())).thenReturn(true);

        TransactionServiceImpl spyService = Mockito.spy(transactionService);
        doReturn(true).when(spyService).callingClientAccountUnblock(transaction.getAccountId());

        spyService.correctionTransaction(transaction, TransactionActionType.CREATE.name());

        verify(correctionTransactionRepository, times(1)).findByCorrectionalTransactionalId(transaction.getTransactionId());
        verify(correctionTransactionRepository, never()).save(any(CorrectionTransaction.class));
        verify(clientAccountService, times(1)).checkClientAccountStateById(transaction.getAccountId());
    }

    @Test
    void testCorrectionTransaction_ExistingCorrectionTransaction_AccountIsBlocked_AccountNotUnblocked() {
        CorrectionTransaction existingCorrectionTransaction = new CorrectionTransaction();
        existingCorrectionTransaction.setTransactionId(transaction.getTransactionId());

        when(correctionTransactionRepository.findByCorrectionalTransactionalId(transaction.getTransactionId()))
                .thenReturn(Optional.of(existingCorrectionTransaction));
        when(clientAccountService.checkClientAccountStateById(transaction.getAccountId())).thenReturn(true);

        TransactionServiceImpl spyService = Mockito.spy(transactionService);
        doReturn(false).when(spyService).callingClientAccountUnblock(transaction.getAccountId());

        spyService.correctionTransaction(transaction, TransactionActionType.CREATE.name());

        verify(correctionTransactionRepository, times(1)).findByCorrectionalTransactionalId(transaction.getTransactionId());
        verify(correctionTransactionRepository, never()).deleteById(anyLong());
        verify(clientAccountService, times(1)).checkClientAccountStateById(transaction.getAccountId());
    }

    @Test
    void testCorrectionTransaction_ExistingCorrectionTransaction_AccountIsBlocked_AccountUnblocked() {
        CorrectionTransaction existingCorrectionTransaction = new CorrectionTransaction();
        existingCorrectionTransaction.setTransactionId(transaction.getTransactionId());

        when(correctionTransactionRepository.findByCorrectionalTransactionalId(transaction.getTransactionId()))
                .thenReturn(Optional.of(existingCorrectionTransaction));
        when(clientAccountService.checkClientAccountStateById(transaction.getAccountId())).thenReturn(true);

        TransactionServiceImpl spyService = Mockito.spy(transactionService);
        doReturn(true).when(spyService).callingClientAccountUnblock(transaction.getAccountId());

        spyService.correctionTransaction(transaction, TransactionActionType.CREATE.name());

        verify(correctionTransactionRepository, times(1)).findByCorrectionalTransactionalId(transaction.getTransactionId());
        verify(correctionTransactionRepository, times(1)).deleteById(Long.valueOf(existingCorrectionTransaction.getTransactionId()));
        verify(clientAccountService, times(1)).checkClientAccountStateById(transaction.getAccountId());
    }


}