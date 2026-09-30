package com.bankingapp.dto;

import com.bankingapp.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JSON shape for a single ledger row, returned both right after a
 * deposit/withdraw/transfer call and in the transaction history list.
 */
public class TransactionResponse {

    private Long id;
    private String accountNumber;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfterTransaction;
    private String description;
    private String relatedAccountNumber;
    private LocalDateTime createdAt;

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, String accountNumber, TransactionType transactionType, BigDecimal amount,
                                BigDecimal balanceAfterTransaction, String description,
                                String relatedAccountNumber, LocalDateTime createdAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfterTransaction = balanceAfterTransaction;
        this.description = description;
        this.relatedAccountNumber = relatedAccountNumber;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfterTransaction() {
        return balanceAfterTransaction;
    }

    public String getDescription() {
        return description;
    }

    public String getRelatedAccountNumber() {
        return relatedAccountNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
