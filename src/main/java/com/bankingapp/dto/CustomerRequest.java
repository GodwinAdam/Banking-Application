package com.bankingapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * The JSON shape a client must POST to /api/customers to create a customer.
 *
 * Why a separate class from the Customer entity?
 * - The entity has fields (id, createdAt) the client must never set.
 * - Validation rules belong to "what a request must look like", which is a
 *   different concern from "what a database row looks like".
 * - It decouples our API contract from our schema: we can change the table
 *   structure without automatically changing (or breaking) the public API.
 */
public class CustomerRequest {

    @NotBlank(message = "fullName is required")
    // @NotBlank rejects null, empty string "", and whitespace-only " ".
    // If this fails, Spring throws a MethodArgumentNotValidException,
    // which our GlobalExceptionHandler turns into a 400 response.
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    // @Email checks the string matches a standard email pattern
    // (something@something.tld). It only runs its check if the value is
    // non-null, which is why we still need @NotBlank alongside it.
    private String email;

    @NotBlank(message = "phoneNumber is required")
    private String phoneNumber;

    private String address; // optional, so no validation annotation

    // No-arg constructor: Jackson (the JSON library) needs this to create
    // an empty instance before populating fields from the incoming JSON.
    public CustomerRequest() {
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
}
