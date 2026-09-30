package com.bankingapp.controller;

import com.bankingapp.dto.DepositRequest;
import com.bankingapp.dto.TransactionResponse;
import com.bankingapp.dto.TransferRequest;
import com.bankingapp.entity.enums.TransactionType;
import com.bankingapp.exception.InsufficientBalanceException;
import com.bankingapp.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    void deposit_returns200_withUpdatedBalance() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setAccountNumber("AC1000");
        request.setAmount(new BigDecimal("50.00"));

        TransactionResponse response = new TransactionResponse(
                1L, "AC1000", TransactionType.DEPOSIT, new BigDecimal("50.00"),
                new BigDecimal("150.00"), "test", null, LocalDateTime.now());
        when(transactionService.deposit(any())).thenReturn(response);

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfterTransaction").value(150.00));
    }

    @Test
    void deposit_returns400_whenAmountIsZeroOrNegative() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setAccountNumber("AC1000");
        request.setAmount(new BigDecimal("0.00")); // violates @DecimalMin("0.01")

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_returns409_whenInsufficientBalance() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("AC1000");
        request.setToAccountNumber("AC1001");
        request.setAmount(new BigDecimal("99999.00"));

        when(transactionService.transfer(any()))
                .thenThrow(new InsufficientBalanceException("Insufficient balance in account AC1000"));

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Insufficient balance in account AC1000"));
    }
}
