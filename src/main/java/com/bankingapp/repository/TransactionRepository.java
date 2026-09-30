package com.bankingapp.repository;

import com.bankingapp.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Reads as: "find Transactions where account.accountNumber = ?,
    // ordered by createdAt descending (newest first)".
    // The "Account_AccountNumber" part walks Transaction -> account (the
    // @ManyToOne field) -> accountNumber (a field on Account).
    List<Transaction> findByAccount_AccountNumberOrderByCreatedAtDesc(String accountNumber);
}
