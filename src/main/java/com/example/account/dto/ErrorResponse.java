package com.example.account.dto;

import com.example.account.type.ErrorCode;
import lombok.*;

import javax.lang.model.type.ErrorType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
    private ErrorCode errorCode;
    private String ErrorMessage;
}
