package com.example.account.service;

import com.example.account.Exception.AccountException;
import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.ErrorCode;
import com.google.common.base.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.AccountStatus.UNREGISTERED;
import static com.example.account.type.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /**
     * 사용자가 있는지 조회
     * 계좌의 번호를 생성
     * 해당 계좌 저장, 해당 정보를 넘김.
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        AccountUser accountUser = getAccountUser(userId);

        validateCreateAccount(accountUser);
        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> String.valueOf(Long.parseLong(account.getAccountNumber()) + 1))
                .orElse(generateRandomAccountNumber());


        return AccountDto.fromEntity(
                accountRepository.save(
                        Account.builder()
                                .accountUser(accountUser)
                                .accountStatus(IN_USE)
                                .accountNumber(newAccountNumber)
                                .balance(initialBalance)
                                .registeredAt(LocalDateTime.now())
                                .build())
        );
    }


    private String generateRandomAccountNumber() {
        Random random = new Random();
        // 0부터 9999999999까지의 랜덤 숫자를 생성
        long randomNumber = (long) (random.nextDouble() * 10000000000L);
        // 10자리로 포맷하여 반환
        return String.format("%010d", randomNumber);
    }

    private AccountUser getAccountUser(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        return accountUser;
    }

    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = getAccountUser(userId);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }



    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser) >= 10) {
            throw new AccountException(ErrorCode.MAX_ACCOUNT_PER_USER_ID);
        }
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        if(!Objects.equal(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() == UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() > 0) {
            throw new AccountException(ErrorCode.BALANCE_IS_NOT_EMPTY);
        }
    }

    @Transactional
    public Account getAccount(Long id) {
        if (id < 0) {
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }

    @Transactional
    public List<AccountDto> getAccountsByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);

        List<Account> accounts = accountRepository.findAllByAccountUser(accountUser);

        return accounts.stream().
                map(AccountDto::fromEntity).
                collect(Collectors.toList());
    }
}
