package com.bankingapp.entity;

import com.bankingapp.entity.enums.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps to the "transactions" table - an append-only ledger. We never
 * UPDATE or DELETE a Transaction row once written; that immutability is
 * what makes it trustworthy as an audit trail, exactly like a real bank
 * statement.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after_transaction", nullable = false, precision = 19, scale = 2)
    // Storing the resulting balance on every row (rather than recomputing
    // it later by summing all prior transactions) makes "what was my
    // balance after transaction #42" an O(1) read instead of an O(n) scan.
    private BigDecimal balanceAfterTransaction;

    @Column(name = "description")
    private String description;

    @Column(name = "related_account_number")
    // Only populated for TRANSFER_OUT / TRANSFER_IN rows, so a customer
    // can see "who did this money go to / come from".
    private String relatedAccountNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Transaction() {
    }

    public Transaction(Account account, TransactionType transactionType, BigDecimal amount,
                        BigDecimal balanceAfterTransaction, String description, String relatedAccountNumber) {
        this.account = account;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfterTransaction = balanceAfterTransaction;
        this.description = description;
        this.relatedAccountNumber = relatedAccountNumber;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
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
