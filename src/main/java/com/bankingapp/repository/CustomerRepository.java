package com.bankingapp.repository;

import com.bankingapp.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * We write ZERO implementation here. Spring Data JPA generates a runtime
 * proxy implementing this interface at startup, based purely on:
 *   1. Extending JpaRepository<Customer, Long> - which alone gives us
 *      save(), findById(), findAll(), deleteById(), count(), etc. for free.
 *   2. Method NAME CONVENTIONS - "findByEmail" is parsed word-by-word
 *      ("find" + "By" + "Email") and translated into
 *      "SELECT * FROM customers WHERE email = ?".
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Optional<T> forces callers to explicitly handle the "no customer
    // with this email" case instead of risking a NullPointerException.
    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);
}
