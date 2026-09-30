package com.bankingapp.repository;

import com.bankingapp.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    // Spring Data parses "findByCustomerId" by following the object graph:
    // Account -> customer (the @ManyToOne field) -> id. It becomes:
    // SELECT * FROM accounts WHERE customer_id = ?
    List<Account> findByCustomerId(Long customerId);
}
