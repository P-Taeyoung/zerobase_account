package com.example.account.controller;

import com.example.account.domain.Account;
import com.example.account.dto.CancelBalance;
import com.example.account.dto.QueryTransactionResponse;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.UseBalance;
import com.example.account.service.TransactionService;
import com.example.account.type.AccountStatus;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successUseBalanceTransaction() throws Exception {
        //given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
                .willReturn(
                        TransactionDto.builder()
                                .accountNumber("1000000000")
                                .transactionResultType(TransactionResultType.SUCCESS)
                                .transactionId("asdasd")
                                .amount(3000L)
                                .build()
                );
        //when
        //then
        mockMvc.perform(post("/use_balance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new UseBalance.Request(
                                1L, "1000000001", 3000L))
                ))
                .andDo(print())
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionResultType").value(TransactionResultType.SUCCESS.toString()))
                .andExpect(jsonPath("$.transactionId").value("asdasd"))
                .andExpect(jsonPath("$.amount").value(3000L));

    }

    @Test
    void successCancelBalanceTransaction() throws Exception {
        //given
        given(transactionService.cancelBalance(anyString(), anyString(), anyLong()))
                .willReturn(
                        TransactionDto.builder()
                                .accountNumber("1000000000")
                                .transactionResultType(TransactionResultType.SUCCESS)
                                .transactionId("asdasd")
                                .amount(3000L)
                                .build()
                );
        //when
        //then
        mockMvc.perform(post("/cancel_balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CancelBalance.Request(
                                        "asdasd", "1000000000", 3000L))
                        ))
                .andDo(print())
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionResultType").value(TransactionResultType.SUCCESS.toString()))
                .andExpect(jsonPath("$.transactionId").value("asdasd"))
                .andExpect(jsonPath("$.amount").value(3000L));

    }

    @Test
    void successGetTransaction() throws Exception {
        //given
        given(transactionService.queryTransaction(anyString()))
                .willReturn(TransactionDto.builder()
                        .transactionType(TransactionType.USE_BALANCE)
                        .transactionId("asdasd")
                        .accountNumber("1000000000")
                        .transactionResultType(TransactionResultType.SUCCESS)
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .build());

        //when
        //then
        mockMvc.perform(get("/transaction/asdasdasd"))
                .andDo(print())
                .andExpect(jsonPath("$.transactionType").value(TransactionType.USE_BALANCE.toString()))
                .andExpect(jsonPath("$.transactionResultType").value(TransactionResultType.SUCCESS.toString()))
                .andExpect(jsonPath("$.transactionId").value("asdasd"))
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(status().isOk());
    }


}