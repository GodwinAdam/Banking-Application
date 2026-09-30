package com.bankingapp.service.impl;

import com.bankingapp.dto.AccountRequest;
import com.bankingapp.dto.AccountResponse;
import com.bankingapp.entity.Account;
import com.bankingapp.entity.Customer;
import com.bankingapp.entity.Transaction;
import com.bankingapp.entity.enums.TransactionType;
import com.bankingapp.exception.ResourceNotFoundException;
import com.bankingapp.repository.AccountRepository;
import com.bankingapp.repository.CustomerRepository;
import com.bankingapp.repository.TransactionRepository;
import com.bankingapp.service.AccountService;
import com.bankingapp.util.AccountNumberGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    public AccountServiceImpl(AccountRepository accountRepository, CustomerRepository customerRepository,
                               TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    // Two writes happen here whenever initialDeposit > 0: the new Account
    // row and its opening Transaction row. @Transactional keeps them
    // atomic, the same reasoning as in TransactionServiceImpl.
    public AccountResponse openAccount(AccountRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + request.getCustomerId()));

        // Loop instead of trusting one random draw: collisions are
        // astronomically unlikely (1 in 90 million) but if one ever did
        // happen, we regenerate rather than violating the accounts table's
        // UNIQUE constraint on account_number.
        String accountNumber;
        do {
            accountNumber = AccountNumberGenerator.generate();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        Account account = new Account(accountNumber, request.getAccountType(),
                request.getInitialDeposit(), customer);

        Account saved = accountRepository.save(account);

        // Every balance-affecting event must leave a ledger trace - an
        // account opened with $1000 should show that $1000 in its
        // transaction history, exactly like a real bank statement does,
        // instead of the balance just "appearing" with no paper trail.
        if (request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            Transaction openingDeposit = new Transaction(
                    saved,
                    TransactionType.DEPOSIT,
                    request.getInitialDeposit(),
                    saved.getBalance(),
                    "Initial deposit on account opening",
                    null
            );
            transactionRepository.save(openingDeposit);
        }

        return toResponse(saved);
    }

    @Override
    public AccountResponse getAccountByNumber(String accountNumber) {
        return toResponse(getAccountEntityByNumber(accountNumber));
    }

    @Override
    public List<AccountResponse> getAccountsByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Account getAccountEntityByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with account number: " + accountNumber));
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getCustomer().getId(),
                account.getCustomer().getFullName(),
                account.getCreatedAt()
        );
    }
}
