package com.bankingapp.entity;

import com.bankingapp.entity.enums.AccountType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps to the "accounts" table. Each Account belongs to exactly one
 * Customer (many-to-one) and owns many Transactions (one-to-many).
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 12)
    // This is the human-facing identifier customers actually use (unlike
    // the internal numeric "id"). We generate it ourselves - see
    // AccountServiceImpl - rather than letting the database auto-increment
    // it, because it needs a specific readable format (e.g. "AC10023456").
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    // EnumType.STRING stores the human-readable name ("SAVINGS") in the
    // column instead of EnumType.ORDINAL's fragile integer index (0, 1...),
    // which would silently break if we ever reordered the enum constants.
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    // BigDecimal, never double/float, for money: floating point binary
    // representations cannot exactly represent values like 0.1, which
    // leads to cent-level rounding errors compounding over many
    // transactions. precision=19, scale=2 -> up to 17 digits before the
    // decimal point and exactly 2 after it.
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY)
    // FetchType.LAZY -> loading an Account does NOT automatically load its
    // Customer from the database; it's fetched only when .getCustomer() is
    // actually called. This avoids pulling the whole customer graph every
    // time we just need an account's balance.
    @JoinColumn(name = "customer_id", nullable = false)
    // @JoinColumn declares the actual foreign-key column ("customer_id")
    // that lives in the "accounts" table and points at customers.id.
    private Customer customer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    protected Account() {
    }

    public Account(String accountNumber, AccountType accountType, BigDecimal balance, Customer customer) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.customer = customer;
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        // Package-private-in-spirit setter: only the service layer should
        // ever call this, and only inside a @Transactional method, so the
        // balance change and its Transaction ledger row are saved atomically.
        this.balance = balance;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}
