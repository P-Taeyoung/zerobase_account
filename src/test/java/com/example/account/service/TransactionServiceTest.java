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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.TransactionResultType.FAILED;
import static com.example.account.type.TransactionResultType.SUCCESS;
import static com.example.account.type.TransactionType.CANCEL_USE_BALANCE;
import static com.example.account.type.TransactionType.USE_BALANCE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("POBI").build();
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(IN_USE)
                .accountNumber("1000000000")
                .balance(10000L).build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(
                        Transaction.builder()
                                .transactionType(USE_BALANCE)
                                .transactionResultType(SUCCESS)
                                .account(account)
                                .amount(200L)
                                .balanceSnapshot(9900L)
                                .transactedAt(LocalDateTime.now())
                                .transactionId("transactionId")
                                .build()
                );

        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        //when

        TransactionDto transactionDto = transactionService.useBalance(
                1L, "1000000000", 200L);
        //then
        verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());
        assertEquals(9800L, transactionArgumentCaptor.getValue().getBalanceSnapshot());
        assertEquals(SUCCESS, transactionDto.getTransactionResultType());
        assertEquals(9900L, transactionDto.getBalanceSnapshot());
        assertEquals(USE_BALANCE, transactionDto.getTransactionType());
    }

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void useBalance_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 200L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }


    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
    void deleteAccount_accountNotFound() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("POBI").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 200L));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해당 유저와 계좌 정보 유저와 불일치 - 잔액 사용 실패")
    void deleteAccount_userUnMatch() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("POBI").build();
        AccountUser harry = AccountUser.builder()
                .id(13L)
                .name("HARRY").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(harry)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 200L));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 상태 - 잔액 사용 실패")
    void deleteAccount_alreadyUnregistered() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("POBI").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 200L));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, accountException.getErrorCode());
    }



    @Test
    @DisplayName("사용 금액이 잔액보다 큼 - 잔액 사용 실패")
    void exceedAmount_UseBalance() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("POBI").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(IN_USE)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 200L));

        //then
        verify(accountUserRepository, times(0)).save(any());
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, accountException.getErrorCode());
    }

    @Test
    @DisplayName("사용 금액이 너무 큼 - 잔액 사용 실패")
    void AmountTooBig_UseBalance() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("POBI").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(IN_USE)
                        .balance(2500000L)
                        .accountNumber("1000000012")
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 2000010L));

        //then
        verify(accountUserRepository, times(0)).save(any());
        assertEquals(ErrorCode.INVALID_REQUEST, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래실패내역 저장")
    void saveFailedUseTransaction() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("POBI").build();
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(IN_USE)
                .accountNumber("1000000000")
                .balance(10000L).build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(
                        Transaction.builder()
                                .transactionType(USE_BALANCE)
                                .transactionResultType(FAILED)
                                .account(account)
                                .amount(10200L)
                                .balanceSnapshot(10000L)
                                .transactedAt(LocalDateTime.now())
                                .transactionId("transactionId")
                                .build()
                );

        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        //when
        transactionService.saveFailedUseTransaction(
                "1000000000", 10200L);
        //then
        verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());
        assertEquals(10000L, transactionArgumentCaptor.getValue().getBalanceSnapshot());
        assertEquals(10200L, transactionArgumentCaptor.getValue().getAmount());
        assertEquals(FAILED, transactionArgumentCaptor.getValue().getTransactionResultType());
    }

    @Test
    void successCancelBalance() {
        //given
        Account account = Account.builder()
                .accountStatus(IN_USE)
                .accountNumber("1000000000")
                .balance(9600L).build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(
                        Optional.of(
                                Transaction.builder()
                                        .transactionType(USE_BALANCE)
                                        .transactionResultType(SUCCESS)
                                        .account(account)
                                        .amount(200L)
                                        .balanceSnapshot(9800L)
                                        .transactedAt(LocalDateTime.now())
                                        .transactionId("transactionId")
                                        .build()
                        )
                );
        given(transactionRepository.save(any()))
                .willReturn(
                        Transaction.builder()
                                .transactionType(CANCEL_USE_BALANCE)
                                .transactionResultType(SUCCESS)
                                .account(account)
                                .amount(200L)
                                .balanceSnapshot(10000L)
                                .transactedAt(LocalDateTime.now())
                                .transactionId("transactionId")
                                .build()
                );

        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        //when

        TransactionDto transactionDto = transactionService.cancelBalance(
                "transactionId", "1000000000", 200L);
        //then
        verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());
        assertEquals(9800L, transactionArgumentCaptor.getValue().getBalanceSnapshot());
        // transaction 에서 balanceSnapshot이 저장될때 account.getBalance() 를 가져와서
        //저장되기 때문에 예상값을 작성할 때 transaction 값을 참고하는 것이 아니라 Account 값을 참고해야 함.
        assertEquals(SUCCESS, transactionDto.getTransactionResultType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(CANCEL_USE_BALANCE, transactionDto.getTransactionType());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 거래 취소 실패")
    void cancelTransaction_accountNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1000000000", 200L));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해당 거래 없음 - 거래 취소 실패")
    void cancelTransaction_transactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1000000000", 200L));
        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래와 계좌 매칭 실패 - 거래 취소 실패")
    void cancelTransaction_transactionAccountUnMatch() {
        Account account1 = Account.builder()
                .id(1L)
                .accountStatus(IN_USE)
                .accountNumber("1000000000")
                .balance(20000L).build();
        Account account2 = Account.builder()
                .id(2L)
                .accountStatus(IN_USE)
                .accountNumber("1000000001")
                .balance(10000L).build();
        Transaction transaction = Transaction.builder()
                .transactionType(USE_BALANCE)
                .transactionResultType(SUCCESS)
                .account(account2)
                .amount(200L)
                .balanceSnapshot(9800L)
                .transactedAt(LocalDateTime.now())
                .transactionId("transactionId")
                .build();

        //given
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account1));
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1000000001", 200L));
        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액 맞지 않음 - 거래 취소 실패")
    void cancelTransaction_transactionAmountDifferent() {
        Account account1 = Account.builder()
                .id(1L)
                .accountStatus(IN_USE)
                .accountNumber("1000000000")
                .balance(20000L).build();
        Transaction transaction = Transaction.builder()
                .transactionType(USE_BALANCE)
                .transactionResultType(SUCCESS)
                .account(account1)
                .amount(200L)
                .balanceSnapshot(9800L)
                .transactedAt(LocalDateTime.now())
                .transactionId("transactionId")
                .build();

        //given
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account1));
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1000000001", 400L));
        //then
        assertEquals(ErrorCode.TRANSACTION_AMOUNT_DIFFERENT, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래 취소 가능 기간 초과 - 거래 취소 실패")
    void cancelTransaction_transactionOutdated() {
        Account account1 = Account.builder()
                .id(1L)
                .accountStatus(IN_USE)
                .accountNumber("1000000000")
                .balance(20000L).build();
        Transaction transaction = Transaction.builder()
                .transactionType(USE_BALANCE)
                .transactionResultType(SUCCESS)
                .account(account1)
                .amount(200L)
                .balanceSnapshot(9800L)
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(10))
                .transactionId("transactionId")
                .build();

        //given
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account1));
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1000000001", 200L));
        //then
        assertEquals(ErrorCode.TRANSACTION_OUTDATED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래 아이디 조회")
    void successQueryTransaction() {
        Account account = Account.builder()
                .id(2L)
                .accountStatus(IN_USE)
                .accountNumber("1000000001")
                .balance(10000L).build();
        Transaction transaction = Transaction.builder()
                .transactionType(USE_BALANCE)
                .transactionResultType(SUCCESS)
                .account(account)
                .amount(200L)
                .balanceSnapshot(9800L)
                .transactedAt(LocalDateTime.now())
                .transactionId("transactionId")
                .build();

        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        //when
        TransactionDto transactionDto = transactionService
                .queryTransaction("asdasd");
        //then
        assertEquals(USE_BALANCE, transactionDto.getTransactionType());
        assertEquals(200L, transactionDto.getAmount());
        assertEquals(SUCCESS, transactionDto.getTransactionResultType());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("해당 거래 없음 - 거래 조회 실패")
    void queryTransaction_transactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction(
                        "transactionId"));
        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }
}