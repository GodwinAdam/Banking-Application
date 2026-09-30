package com.bankingapp.service.impl;

import com.bankingapp.dto.CustomerRequest;
import com.bankingapp.dto.CustomerResponse;
import com.bankingapp.entity.Customer;
import com.bankingapp.exception.DuplicateResourceException;
import com.bankingapp.exception.ResourceNotFoundException;
import com.bankingapp.repository.CustomerRepository;
import com.bankingapp.service.CustomerService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
// @Service marks this as a Spring-managed bean that holds business logic
// (as opposed to @Repository for data access or @RestController for HTTP
// handling). Functionally identical to @Component, but the distinct name
// documents the class's role and lets Spring apply @Service-specific
// tooling (e.g. exception translation) if configured.
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    // Constructor injection: Spring sees this is the only constructor and
    // automatically supplies a CustomerRepository bean when it creates this
    // service. Preferred over @Autowired on a field because:
    //   1. The field can be declared `final` (immutable after construction).
    //   2. Tests can call `new CustomerServiceImpl(mockRepo)` directly
    //      without needing a Spring container at all.
    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "A customer with email '" + request.getEmail() + "' already exists");
        }

        Customer customer = new Customer(
                request.getFullName(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getAddress()
        );

        // .save() performs an INSERT (since customer.id is null) and, once
        // Hibernate flushes it, populates the generated id back onto the
        // same object we passed in.
        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    @Override
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                // .orElseThrow(...) - if the Optional is empty, run this
                // lambda and throw whatever it returns instead of returning
                // null. This is how we turn "not found" into a clean 404
                // instead of a NullPointerException three lines later.
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return toResponse(customer);
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CustomerResponse toResponse(Customer customer) {
        // Manual entity -> DTO mapping. For a learning project this is
        // deliberately explicit (no MapStruct/ModelMapper magic) so every
        // field's origin is visible at a glance.
        return new CustomerResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getAddress(),
                customer.getCreatedAt()
        );
    }
}
