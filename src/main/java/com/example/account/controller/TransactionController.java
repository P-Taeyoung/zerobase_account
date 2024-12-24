package com.example.account.controller;

import com.example.account.Exception.AccountException;
import com.example.account.aop.AccountLock;
import com.example.account.dto.QueryTransactionResponse;
import com.example.account.dto.UseBalance;
import com.example.account.dto.CancelBalance;
import com.example.account.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/use_balance")
    @AccountLock
    public UseBalance.Response useBalance(
            @RequestBody @Valid UseBalance.Request request
    ) throws InterruptedException {
        Thread.sleep(2000);
        try {
            return UseBalance.Response.from(
                    transactionService.useBalance(request.getUserId(),
                            request.getAccountNumber(), request.getAmount())
            );
        } catch (AccountException e) {
            log.error("Failed to use balance");
            transactionService.saveFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );
            throw e;
        }
    }

    @PostMapping("/cancel_balance")
    @AccountLock
    public CancelBalance.Response response (
            @RequestBody @Valid CancelBalance.Request request
    ) throws InterruptedException {
        Thread.sleep(2000);
        try {
            return CancelBalance.Response.from(
                    transactionService.cancelBalance(request.getTransactionId(),
                            request.getAccountNumber(), request.getAmount()
                    )
            );
        } catch (AccountException e) {
            log.error("Failed to cancel balance");
            transactionService.saveFailedCancelTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );
            throw e;
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public QueryTransactionResponse queryTransactionResponse (
            @PathVariable String transactionId
    ) {
        return QueryTransactionResponse.from(
                transactionService.queryTransaction(transactionId)
        );
    }
}
