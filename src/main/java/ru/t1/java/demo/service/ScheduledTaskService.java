package ru.t1.java.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final ClientAccountService clientAccountService;

    private final TransactionService transactionService;
    @Value("${scheduling.cron.transactions}")
    private String transactionsCron;

    @Value("${scheduling.cron.block-negative-balance}")
    private String blockNegativeBalanceCron;


    @Scheduled(cron = "#{@blockNegativeBalanceCron}")
    public void scheduleBlockNegativeBalanceCreditClientAccounts() {
        log.info("Ночной ежедневный запуск планировщика для блокировки кредитных счетов с отрицательным балансом.");
        clientAccountService.blockNegativeBalanceCreditClientAccounts();
    }

    @Scheduled(cron = "#{@transactionsCron}")
    public void scheduleSendingListOfTransactionsForProcessing() {
        log.info("Ежедневный c 6 утра до 23 вечера ежечасный запуск планировщика для отправки корректируемых транзакций на повторную обработку.");
        transactionService.processingListOfCorrectionTransactions();
    }

}
