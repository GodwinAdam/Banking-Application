package com.bankingapp;

import com.bankingapp.dto.*;
import com.bankingapp.entity.enums.AccountType;
import com.bankingapp.entity.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A true END-TO-END test: @SpringBootTest with webEnvironment = RANDOM_PORT
 * starts the ENTIRE application (real embedded Tomcat on a free port, the
 * real Spring container, every real @Service/@Repository, and a real H2
 * database as configured in src/test/resources/application.yml). Nothing
 * is mocked. TestRestTemplate then makes genuine HTTP calls over the
 * network to that running instance, exactly like Postman/curl would.
 *
 * This is slower than the unit/slice tests above, so we use it sparingly -
 * just enough to prove the whole system wires together and a realistic
 * customer journey (open two accounts, deposit, withdraw, transfer, read
 * history) produces the right numbers end to end.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BankingApplicationIntegrationTest {

    @LocalServerPort
    // Spring Boot picks a random free port for the embedded server during
    // this test and injects it here, avoiding collisions with anything
    // else already listening on 8080.
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void fullCustomerJourney_openAccounts_depositWithdrawTransfer_andReadHistory() {
        // ---- 1. Create two customers ----
        CustomerResponse alice = createCustomer("Alice Anderson", "alice.it@example.com", "1111111111");
        CustomerResponse bob = createCustomer("Bob Baker", "bob.it@example.com", "2222222222");

        // ---- 2. Open one account each ----
        AccountResponse aliceAccount = openAccount(alice.getId(), AccountType.SAVINGS, "1000.00");
        AccountResponse bobAccount = openAccount(bob.getId(), AccountType.CURRENT, "100.00");

        assertThat(aliceAccount.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(bobAccount.getBalance()).isEqualByComparingTo("100.00");

        // ---- 3. Deposit into Alice's account ----
        DepositRequest deposit = new DepositRequest();
        deposit.setAccountNumber(aliceAccount.getAccountNumber());
        deposit.setAmount(new BigDecimal("200.00"));
        deposit.setDescription("Bonus");
        ResponseEntity<TransactionResponse> depositResp = restTemplate.postForEntity(
                url("/api/transactions/deposit"), deposit, TransactionResponse.class);
        assertThat(depositResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(depositResp.getBody().getBalanceAfterTransaction()).isEqualByComparingTo("1200.00");

        // ---- 4. Withdraw from Alice's account ----
        WithdrawRequest withdraw = new WithdrawRequest();
        withdraw.setAccountNumber(aliceAccount.getAccountNumber());
        withdraw.setAmount(new BigDecimal("300.00"));
        ResponseEntity<TransactionResponse> withdrawResp = restTemplate.postForEntity(
                url("/api/transactions/withdraw"), withdraw, TransactionResponse.class);
        assertThat(withdrawResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(withdrawResp.getBody().getBalanceAfterTransaction()).isEqualByComparingTo("900.00");

        // ---- 5. Transfer from Alice to Bob ----
        TransferRequest transfer = new TransferRequest();
        transfer.setFromAccountNumber(aliceAccount.getAccountNumber());
        transfer.setToAccountNumber(bobAccount.getAccountNumber());
        transfer.setAmount(new BigDecimal("400.00"));
        ResponseEntity<TransactionResponse> transferResp = restTemplate.postForEntity(
                url("/api/transactions/transfer"), transfer, TransactionResponse.class);
        assertThat(transferResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(transferResp.getBody().getBalanceAfterTransaction()).isEqualByComparingTo("500.00");

        // ---- 6. Verify final balances via GET ----
        AccountResponse aliceFinal = restTemplate.getForObject(
                url("/api/accounts/" + aliceAccount.getAccountNumber()), AccountResponse.class);
        AccountResponse bobFinal = restTemplate.getForObject(
                url("/api/accounts/" + bobAccount.getAccountNumber()), AccountResponse.class);
        assertThat(aliceFinal.getBalance()).isEqualByComparingTo("500.00");
        assertThat(bobFinal.getBalance()).isEqualByComparingTo("500.00");

        // ---- 7. Verify Alice's full transaction history (newest first) ----
        ResponseEntity<TransactionResponse[]> historyResp = restTemplate.getForEntity(
                url("/api/transactions/" + aliceAccount.getAccountNumber()), TransactionResponse[].class);
        List<TransactionResponse> history = List.of(historyResp.getBody());
        assertThat(history).hasSize(4);
        assertThat(history.get(0).getTransactionType()).isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(history.get(1).getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(history.get(2).getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(history.get(3).getTransactionType()).isEqualTo(TransactionType.DEPOSIT); // opening deposit

        // ---- 8. Error case: withdrawing more than the balance is a 409 ----
        WithdrawRequest tooMuch = new WithdrawRequest();
        tooMuch.setAccountNumber(bobAccount.getAccountNumber());
        tooMuch.setAmount(new BigDecimal("999999.00"));
        ResponseEntity<ApiError> insufficientResp = restTemplate.postForEntity(
                url("/api/transactions/withdraw"), tooMuch, ApiError.class);
        assertThat(insufficientResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // ---- 9. Error case: looking up a non-existent customer is a 404 ----
        ResponseEntity<ApiError> notFoundResp = restTemplate.getForEntity(
                url("/api/customers/999999"), ApiError.class);
        assertThat(notFoundResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // ---- 10. Error case: duplicate email on customer creation is a 409 ----
        CustomerRequest duplicate = new CustomerRequest();
        duplicate.setFullName("Someone Else");
        duplicate.setEmail("alice.it@example.com"); // same email as step 1
        duplicate.setPhoneNumber("3333333333");
        ResponseEntity<ApiError> duplicateResp = restTemplate.postForEntity(
                url("/api/customers"), duplicate, ApiError.class);
        assertThat(duplicateResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private CustomerResponse createCustomer(String name, String email, String phone) {
        CustomerRequest request = new CustomerRequest();
        request.setFullName(name);
        request.setEmail(email);
        request.setPhoneNumber(phone);
        ResponseEntity<CustomerResponse> response = restTemplate.postForEntity(
                url("/api/customers"), request, CustomerResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private AccountResponse openAccount(Long customerId, AccountType type, String initialDeposit) {
        AccountRequest request = new AccountRequest();
        request.setCustomerId(customerId);
        request.setAccountType(type);
        request.setInitialDeposit(new BigDecimal(initialDeposit));
        ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                url("/api/accounts"), request, AccountResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }
}
