package com.bankingapp.controller;

import com.bankingapp.dto.AccountRequest;
import com.bankingapp.dto.AccountResponse;
import com.bankingapp.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Open accounts and look up balances")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    // POST /api/accounts - opens a new account for an existing customer
    @Operation(summary = "Open a new account for a customer")
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody AccountRequest request) {
        AccountResponse response = accountService.openAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{accountNumber}")
    // GET /api/accounts/AC12345678
    @Operation(summary = "Get account details (including balance) by account number")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @GetMapping("/customer/{customerId}")
    // GET /api/accounts/customer/5 - every account owned by customer 5
    @Operation(summary = "List every account belonging to a customer")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(accountService.getAccountsByCustomer(customerId));
    }
}
