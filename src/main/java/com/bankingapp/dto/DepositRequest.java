package com.bankingapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** JSON body for POST /api/transactions/deposit */
public class DepositRequest {

    @NotBlank(message = "accountNumber is required")
    private String accountNumber;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    // DecimalMin("0.01") rather than @Positive because @Positive on a
    // BigDecimal still allows 0 to be rejected only if inclusive=false is
    // set correctly; being explicit about the smallest currency unit here
    // reads more clearly for a money field.
    private BigDecimal amount;

    private String description;

    public DepositRequest() {
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
