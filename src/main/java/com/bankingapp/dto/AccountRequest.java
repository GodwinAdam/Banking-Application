package com.bankingapp.dto;

import com.bankingapp.entity.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * JSON shape a client POSTs to /api/accounts to open a new account for an
 * existing customer.
 */
public class AccountRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotNull(message = "accountType is required")
    // Jackson automatically converts the JSON string "SAVINGS" into the
    // AccountType.SAVINGS enum constant; if the string doesn't match any
    // constant, Jackson throws an error before validation even runs, which
    // our GlobalExceptionHandler also catches (see HttpMessageNotReadableException).
    private AccountType accountType;

    @NotNull(message = "initialDeposit is required")
    @DecimalMin(value = "0.0", message = "initialDeposit cannot be negative")
    private java.math.BigDecimal initialDeposit;

    public AccountRequest() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public java.math.BigDecimal getInitialDeposit() {
        return initialDeposit;
    }

    public void setInitialDeposit(java.math.BigDecimal initialDeposit) {
        this.initialDeposit = initialDeposit;
    }
}
