package com.example.account.service;

import com.example.account.Exception.AccountException;
import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.account.type.TransactionResultType.FAILED;
import static com.example.account.type.TransactionResultType.SUCCESS;
import static com.example.account.type.TransactionType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final AccountUserRepository accountUserRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionDto useBalance(Long useId, String accountNumber, Long amount) {
        AccountUser accountUser = accountUserRepository.findById(useId).
                orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber).
                orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validatedUseBalance(accountUser, account, amount);

        account.useBalance(amount);
        // 중요한 데이터를 변경할 필요가 있을 때는 데이터를 직접 가져와서 변경하는 것보다는 직접 해당
        // 데이터안에서 변경되도록 하는 것이 바람직.

        return TransactionDto.fromEntity(
                getSaveAndGetTransaction(USE_BALANCE, SUCCESS, account, amount));
    }

    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber).
                orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        getSaveAndGetTransaction(USE_BALANCE, FAILED, account, amount);
    }

    private void validatedUseBalance(AccountUser accountUser, Account account, Long amount) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() != AccountStatus.IN_USE) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() < amount) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
        if (amount < 0 || amount > 2000000L) {
            throw new AccountException(ErrorCode.INVALID_REQUEST);
        }
    }

    @Transactional
    public TransactionDto cancelBalance(String transactionId, String accountNumber, Long amount) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber).
                orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validatedCancelBalance(transaction, account, amount);

        account.cancelBalance(amount);

        return TransactionDto.fromEntity(
                getSaveAndGetTransaction(CANCEL_USE_BALANCE, SUCCESS, account, amount));
    }

    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber).
                orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        getSaveAndGetTransaction(CANCEL_USE_BALANCE, FAILED, account, amount);
    }

    private void validatedCancelBalance(Transaction transaction, Account account, Long amount) {
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH);
        }
        if (!Objects.equals(amount, transaction.getAmount())) {
            throw new AccountException(ErrorCode.TRANSACTION_AMOUNT_DIFFERENT);
        }
        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(ErrorCode.TRANSACTION_OUTDATED);
        }
        if (amount < 0 || amount > 2000000) {
            throw new AccountException(ErrorCode.INVALID_REQUEST);
        }
    }

    public Transaction getSaveAndGetTransaction(TransactionType transactionType,
                                                TransactionResultType transactionResultType,
                                                Account account,
                                                Long amount) {
        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResultType(transactionResultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactedAt(LocalDateTime.now())
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .build()
        );
    }

    @Transactional
    public TransactionDto queryTransaction (String transactionId) {
        return TransactionDto.fromEntity(transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND)));
    }
}
