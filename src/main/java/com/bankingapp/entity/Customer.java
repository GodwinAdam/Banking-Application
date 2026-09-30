package com.bankingapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps to the "customers" table. One Customer can own many Accounts
 * (one-to-many relationship, mirrored on the Account side).
 */
@Entity
// @Entity tells Hibernate "this class is a database table". Without it,
// Hibernate would never know this class exists.
@Table(name = "customers")
// @Table lets us pick the exact table name. If omitted, Hibernate would
// default to the class name ("Customer"), which is fine too - but being
// explicit avoids surprises later if the class is renamed.
public class Customer {

    @Id
    // @Id marks this field as the table's primary key.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // IDENTITY = let the database itself generate the value (e.g. H2/MySQL
    // AUTO_INCREMENT). We never set this field manually.
    private Long id;

    @Column(name = "full_name", nullable = false)
    // nullable = false becomes a NOT NULL constraint in the generated
    // "create table" DDL - the database itself will reject a null value.
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    // unique = true adds a UNIQUE constraint, so two customers can never
    // share the same email at the database level (belt-and-braces on top
    // of the service-layer check we also do).
    private String email;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "created_at", nullable = false, updatable = false)
    // updatable = false means Hibernate will never include this column in
    // an UPDATE statement once the row is inserted - it's a "write once"
    // audit timestamp.
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    // mappedBy = "customer" -> the FOREIGN KEY column lives on the Account
    // side (see Account.customer below); Customer does not own the
    // relationship, it's just the "many" side looking back.
    // cascade = ALL -> saving/deleting a Customer cascades to its Accounts.
    // orphanRemoval = true -> removing an Account from this list deletes it.
    private List<Account> accounts = new ArrayList<>();

    // JPA requires a no-argument constructor so Hibernate can instantiate
    // the entity via reflection before populating its fields.
    protected Customer() {
    }

    public Customer(String fullName, String email, String phoneNumber, String address) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    // Hibernate calls any method annotated @PrePersist right before the
    // INSERT statement runs. This is a safety net in case createdAt was
    // somehow left null (e.g. built via the protected no-arg constructor).
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // --- Getters and setters ---
    // Hibernate and Jackson (JSON serialization) both rely on these plain
    // getter/setter methods rather than direct field access.

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Account> getAccounts() {
        return accounts;
    }
}
