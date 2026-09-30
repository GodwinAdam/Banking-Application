package com.bankingapp.service;

import com.bankingapp.dto.CustomerRequest;
import com.bankingapp.dto.CustomerResponse;
import com.bankingapp.entity.Customer;
import com.bankingapp.exception.DuplicateResourceException;
import com.bankingapp.exception.ResourceNotFoundException;
import com.bankingapp.repository.CustomerRepository;
import com.bankingapp.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * A "unit" test: it exercises CustomerServiceImpl completely in isolation,
 * with NO Spring container and NO real database. CustomerRepository is
 * replaced by a Mockito mock we fully control, so we can test the
 * service's logic (validation, exception throwing, mapping) in
 * milliseconds without ever touching H2/MySQL.
 */
@ExtendWith(MockitoExtension.class)
// Activates Mockito's JUnit 5 integration: it scans this class for
// @Mock/@InjectMocks fields and wires them up before each @Test method.
class CustomerServiceImplTest {

    @Mock
    // Creates a fake CustomerRepository. By default every method returns
    // null / false / empty collections until we explicitly program a
    // behavior with when(...).thenReturn(...) inside a test.
    private CustomerRepository customerRepository;

    @InjectMocks
    // Creates a REAL CustomerServiceImpl and injects the @Mock above into
    // its constructor automatically (Mockito matches the mock to the
    // constructor parameter type).
    private CustomerServiceImpl customerService;

    @Test
    void createCustomer_savesAndReturnsResponse_whenEmailIsNew() {
        // ---- Arrange ----
        CustomerRequest request = new CustomerRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPhoneNumber("9876543210");
        request.setAddress("221B Baker Street");

        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(false);
        // When the service calls repository.save(customer), just return
        // whatever Customer object it was given - we don't need Hibernate
        // to actually assign it a real id for this test to be meaningful.
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ---- Act ----
        CustomerResponse response = customerService.createCustomer(request);

        // ---- Assert ----
        assertThat(response.getFullName()).isEqualTo("Jane Doe");
        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        // verify(...) checks a mock method was actually called - proving
        // the service really attempted to persist the new customer.
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createCustomer_throwsDuplicateResourceException_whenEmailAlreadyExists() {
        CustomerRequest request = new CustomerRequest();
        request.setEmail("taken@example.com");
        request.setFullName("Someone");
        request.setPhoneNumber("111");

        when(customerRepository.existsByEmail("taken@example.com")).thenReturn(true);

        // assertThatThrownBy runs the lambda and asserts it throws; if it
        // doesn't throw (or throws the wrong type), the test fails.
        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("taken@example.com");

        // Because we returned early on the duplicate check, save() must
        // NEVER have been called - verify(never()) proves that.
        verify(customerRepository, never()).save(any());
    }

    @Test
    void getCustomerById_returnsCustomer_whenFound() {
        Customer existing = new Customer("John Smith", "john@example.com", "5551234", "Main St");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));

        CustomerResponse response = customerService.getCustomerById(1L);

        assertThat(response.getFullName()).isEqualTo("John Smith");
    }

    @Test
    void getCustomerById_throwsResourceNotFoundException_whenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllCustomers_returnsMappedList() {
        Customer c1 = new Customer("A", "a@example.com", "111", null);
        Customer c2 = new Customer("B", "b@example.com", "222", null);
        when(customerRepository.findAll()).thenReturn(List.of(c1, c2));

        List<CustomerResponse> responses = customerService.getAllCustomers();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CustomerResponse::getEmail)
                .containsExactly("a@example.com", "b@example.com");
    }
}
