package com.bankingapp.controller;

import com.bankingapp.dto.CustomerRequest;
import com.bankingapp.dto.CustomerResponse;
import com.bankingapp.exception.ResourceNotFoundException;
import com.bankingapp.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A "slice" test: @WebMvcTest boots ONLY the web layer (this one
 * controller, Jackson JSON conversion, Bean Validation, and our
 * GlobalExceptionHandler) - no real database, no service implementation,
 * no full Spring Boot startup. This is the fastest way to verify HTTP
 * status codes, JSON shapes, and validation wiring in isolation.
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    // MockMvc lets us fire simulated HTTP requests directly at the
    // controller, in-process, without opening a real network socket.
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // used to turn Java objects into JSON strings

    @MockBean
    // @MockBean replaces the real CustomerService bean in this test's
    // Spring context with a Mockito mock, so CustomerController is tested
    // in complete isolation from the actual business logic/database.
    private CustomerService customerService;

    @Test
    void createCustomer_returns201AndBody_whenRequestIsValid() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPhoneNumber("9876543210");

        CustomerResponse response = new CustomerResponse(
                1L, "Jane Doe", "jane@example.com", "9876543210", null, LocalDateTime.now());
        when(customerService.createCustomer(any())).thenReturn(response);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void createCustomer_returns400_whenEmailIsMissing() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setFullName("Jane Doe");
        // email intentionally left null -> should trip @NotBlank
        request.setPhoneNumber("9876543210");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    void getCustomer_returns200_whenFound() throws Exception {
        CustomerResponse response = new CustomerResponse(
                5L, "John Smith", "john@example.com", "111", null, LocalDateTime.now());
        when(customerService.getCustomerById(5L)).thenReturn(response);

        mockMvc.perform(get("/api/customers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Smith"));
    }

    @Test
    void getCustomer_returns404_whenServiceThrowsNotFound() throws Exception {
        when(customerService.getCustomerById(eq(999L)))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: 999"));

        mockMvc.perform(get("/api/customers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found with id: 999"));
    }
}
