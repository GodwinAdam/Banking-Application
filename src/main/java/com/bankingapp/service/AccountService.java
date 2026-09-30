package com.bankingapp.service;

import com.bankingapp.dto.AccountRequest;
import com.bankingapp.dto.AccountResponse;
import com.bankingapp.entity.Account;

import java.util.List;

public interface AccountService {

    AccountResponse openAccount(AccountRequest request);

    AccountResponse getAccountByNumber(String accountNumber);

    List<AccountResponse> getAccountsByCustomer(Long customerId);

    /**
     * Returns the managed Account ENTITY (not a DTO) because
     * TransactionServiceImpl needs to mutate its balance and persist that
     * change within its own @Transactional method - a DTO can't do that.
     * Package-visible use case only; controllers never call this.
     */
    Account getAccountEntityByNumber(String accountNumber);
}
