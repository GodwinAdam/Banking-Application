package com.bankingapp.controller;

import com.bankingapp.dto.CustomerRequest;
import com.bankingapp.dto.CustomerResponse;
import com.bankingapp.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// @RestController = @Controller + @ResponseBody. It tells Spring MVC:
//   1. This class handles incoming HTTP requests (@Controller part).
//   2. Every method's return value should be written directly into the
//      HTTP response body (serialized to JSON by Jackson), instead of
//      being treated as the name of an HTML view template.
@RequestMapping("/api/customers")
// @RequestMapping here sets the common URL prefix for every method below,
// so we only write the method-specific suffix (e.g. "/{id}") per handler.
@Tag(name = "Customers", description = "Create and look up bank customers")
// @Tag is purely for Swagger UI - it groups these endpoints under a
// "Customers" heading in the generated API documentation page.
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    // @PostMapping (shorthand for @RequestMapping(method = POST)) + the
    // class-level prefix means this method handles: POST /api/customers
    @Operation(summary = "Create a new customer")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        // @RequestBody: deserialize the incoming JSON body into a
        // CustomerRequest object.
        // @Valid: BEFORE this method body runs, validate every constraint
        // annotation (@NotBlank, @Email, ...) on that object; if any fail,
        // Spring throws MethodArgumentNotValidException instead of ever
        // calling this method, and GlobalExceptionHandler turns that into
        // a 400 response.
        CustomerResponse response = customerService.createCustomer(request);

        // HttpStatus.CREATED = 201, the correct status for "a new resource
        // was successfully created", as opposed to the default 200 OK.
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    // Handles: GET /api/customers/5
    @Operation(summary = "Get a customer by id")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        // @PathVariable binds the "{id}" segment of the URL to this
        // method parameter; Spring also converts the String "5" to a Long
        // automatically.
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response); // ResponseEntity.ok(x) == 200 OK with body x
    }

    @GetMapping
    // Handles: GET /api/customers
    @Operation(summary = "List all customers")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }
}
