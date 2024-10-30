package ru.t1.java.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.t1.java.demo.mapper.ClientAccountMapper;
import ru.t1.java.demo.model.dto.ClientAccountDto;
import ru.t1.java.demo.service.ClientAccountService;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("${alias.api.accounts}")
@PreAuthorize("hasRole('MODERATOR')")
public class ClientAccountController {
    private final ClientAccountService clientAccountService;
    private final ClientAccountMapper clientAccountMapper;

    @PostMapping("/create-new-account")
    public ResponseDto createNewAccount(@RequestBody ClientAccountDto clientAccountDto) {
        clientAccountService.saveAccount(clientAccountMapper.toEntity(clientAccountDto));
        log.info("Создан новый счет клиента");
        return new ResponseDto(String.format("Счёт клиента c номером: {%s} успешно создан", clientAccountDto.getClientId()));

    }

    /*
    Изменение типа счета клиента с кредитного на дебитовый и наоборот
     */
    @PatchMapping("/set-debit-credit-account-type/{clientAccountId}")
    public ResponseDto setNewAccountType(@PathVariable Long clientAccountId) {
        clientAccountService.changeClientAccountType(clientAccountId);
        log.info("Тип счета клиента изменен");
        return new ResponseDto(String.format("Тип счёта клиента c номером: {%s} успешно изменен", clientAccountId));

    }

    @PatchMapping("/block-account/{clientAccountId}")
    public ResponseDto blockClientAccount(@PathVariable Long clientAccountId) {
        String blockedMessage = clientAccountService.blockClientAccount(clientAccountId);
        log.info("Счет клиента заблокирован");
        return new ResponseDto(String.format(blockedMessage));
    }

    @PatchMapping("/unblock-account/{clientAccountId}")
    public ResponseDto unblockClientAccount(@PathVariable Long clientAccountId) {
        String unblockedMessage = clientAccountService.unblockClientAccount(clientAccountId);
        log.info("Счет клиента разблокирован");
        return new ResponseDto(String.format(unblockedMessage));
    }


}
