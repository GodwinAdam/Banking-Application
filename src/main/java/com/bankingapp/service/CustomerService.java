package com.bankingapp.service;

import com.bankingapp.dto.CustomerRequest;
import com.bankingapp.dto.CustomerResponse;

import java.util.List;

/**
 * The contract the controller layer depends on. Controllers talk to THIS
 * interface, never to CustomerServiceImpl directly - which means we could
 * swap in a different implementation (e.g. one backed by a cache, or a
 * mock in a test) without touching any controller code.
 */
public interface CustomerService {

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse getCustomerById(Long id);

    List<CustomerResponse> getAllCustomers();
}
