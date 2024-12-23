package com.example.account.type;

import com.example.account.domain.Transaction;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST("잘못된 요청입니다."),
    USER_NOT_FOUND("사용자가 없습니다"),
    ACCOUNT_NOT_FOUND("계좌가 없습니다"),
    ACCOUNT_ALREADY_UNREGISTERED("계좌가 이미 해지되었습니다."),
    USER_ACCOUNT_UN_MATCH("사용자ID 와 계좌 소유주 ID가 일치하지 않습니다."),
    BALANCE_IS_NOT_EMPTY("계좌 잔액이 남아있습니다."),
    AMOUNT_EXCEED_BALANCE("계좌 잔액이 부족합니다."),
    MAX_ACCOUNT_PER_USER_ID("계좌를 더 이상 만들수 없습니다."),
    TRANSACTION_NOT_FOUND("거래 내역을 찾을 수 없습니다."),
    TRANSACTION_ACCOUNT_UN_MATCH("요청 거래 내역의 계좌와 계좌번호와 다릅니다."),
    TRANSACTION_AMOUNT_DIFFERENT("요청 거래 금액과 거래 취소 금액과 다릅니다."),
    TRANSACTION_OUTDATED("요청 거래 일자가 1년이 지났습니다."),
    ;



    private final String description;
}
