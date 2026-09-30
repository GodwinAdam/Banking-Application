package com.bankingapp.service.impl;

import com.bankingapp.dto.DepositRequest;
import com.bankingapp.dto.TransactionResponse;
import com.bankingapp.dto.TransferRequest;
import com.bankingapp.dto.WithdrawRequest;
import com.bankingapp.entity.Account;
import com.bankingapp.entity.Transaction;
import com.bankingapp.entity.enums.TransactionType;
import com.bankingapp.exception.InsufficientBalanceException;
import com.bankingapp.repository.AccountRepository;
import com.bankingapp.repository.TransactionRepository;
import com.bankingapp.service.AccountService;
import com.bankingapp.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransactionServiceImpl(AccountRepository accountRepository,
                                   TransactionRepository transactionRepository,
                                   AccountService accountService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    @Override
    @Transactional
    // @Transactional wraps this whole method in a single database
    // transaction: Spring opens it on entry and either COMMITs (if the
    // method returns normally) or ROLLS BACK every change made inside it
    // (if any RuntimeException escapes). For a deposit that's one entity
    // update (Account.balance) + one insert (Transaction) that must both
    // succeed or both fail together - never "money moved but no ledger row".
    public TransactionResponse deposit(DepositRequest request) {
        Account account = accountService.getAccountEntityByNumber(request.getAccountNumber());

        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                account,
                TransactionType.DEPOSIT,
                request.getAmount(),
                newBalance,
                request.getDescription(),
                null // no related account for a plain deposit
        );
        Transaction saved = transactionRepository.save(transaction);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        Account account = accountService.getAccountEntityByNumber(request.getAccountNumber());

        // compareTo, never == or <, for BigDecimal comparisons: "<" doesn't
        // even compile on BigDecimal, and .equals() would treat 10.0 and
        // 10.00 as different values, which compareTo correctly does not.
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in account " + request.getAccountNumber() +
                            ". Available: " + account.getBalance() + ", requested: " + request.getAmount());
        }

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                account,
                TransactionType.WITHDRAWAL,
                request.getAmount(),
                newBalance,
                request.getDescription(),
                null
        );
        Transaction saved = transactionRepository.save(transaction);

        return toResponse(saved);
    }

    @Override
    @Transactional
    // The most important @Transactional in the app: a transfer touches
    // TWO accounts and writes TWO ledger rows. If withdrawing from the
    // source succeeded but crediting the destination failed for any
    // reason, @Transactional guarantees the whole method rolls back -
    // the source account's debit is undone too, so money can never
    // "vanish" between two accounts.
    public TransactionResponse transfer(TransferRequest request) {
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account fromAccount = accountService.getAccountEntityByNumber(request.getFromAccountNumber());
        Account toAccount = accountService.getAccountEntityByNumber(request.getToAccountNumber());

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in account " + request.getFromAccountNumber() +
                            ". Available: " + fromAccount.getBalance() + ", requested: " + request.getAmount());
        }

        BigDecimal fromNewBalance = fromAccount.getBalance().subtract(request.getAmount());
        fromAccount.setBalance(fromNewBalance);
        accountRepository.save(fromAccount);

        BigDecimal toNewBalance = toAccount.getBalance().add(request.getAmount());
        toAccount.setBalance(toNewBalance);
        accountRepository.save(toAccount);

        Transaction outTransaction = new Transaction(
                fromAccount,
                TransactionType.TRANSFER_OUT,
                request.getAmount(),
                fromNewBalance,
                request.getDescription(),
                toAccount.getAccountNumber()
        );
        transactionRepository.save(outTransaction);

        Transaction inTransaction = new Transaction(
                toAccount,
                TransactionType.TRANSFER_IN,
                request.getAmount(),
                toNewBalance,
                request.getDescription(),
                fromAccount.getAccountNumber()
        );
        transactionRepository.save(inTransaction);

        // We return the sender's side of the transaction, since the caller
        // is the one who initiated (and is paying for) the transfer.
        return toResponse(outTransaction);
    }

    @Override
    // No @Transactional needed: this is a single read-only query, so
    // there's nothing to roll back if it fails partway through.
    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        // Calling getAccountByNumber (not the entity version) purely for
        // its side effect: it throws ResourceNotFoundException if the
        // account doesn't exist, giving a clean 404 instead of silently
        // returning an empty list for a typo'd account number.
        accountService.getAccountByNumber(accountNumber);

        return transactionRepository.findByAccount_AccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getAccountNumber(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceAfterTransaction(),
                transaction.getDescription(),
                transaction.getRelatedAccountNumber(),
                transaction.getCreatedAt()
        );
    }
}
