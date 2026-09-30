package com.bankingapp.service;

import com.bankingapp.dto.DepositRequest;
import com.bankingapp.dto.TransactionResponse;
import com.bankingapp.dto.TransferRequest;
import com.bankingapp.dto.WithdrawRequest;
import com.bankingapp.entity.Account;
import com.bankingapp.entity.Customer;
import com.bankingapp.entity.enums.AccountType;
import com.bankingapp.entity.enums.TransactionType;
import com.bankingapp.exception.InsufficientBalanceException;
import com.bankingapp.repository.AccountRepository;
import com.bankingapp.repository.TransactionRepository;
import com.bankingapp.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    // TransactionServiceImpl depends on the AccountService INTERFACE, not
    // AccountServiceImpl, so we mock the interface - Mockito doesn't care
    // that it's an interface rather than a class.
    private AccountService accountService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Account newAccount(String accountNumber, String balance) {
        Customer customer = new Customer("Test Customer", accountNumber + "@example.com", "000", null);
        return new Account(accountNumber, AccountType.SAVINGS, new BigDecimal(balance), customer);
    }

    @Test
    void deposit_increasesBalance_andRecordsTransaction() {
        Account account = newAccount("AC1000", "100.00");
        when(accountService.getAccountEntityByNumber("AC1000")).thenReturn(account);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DepositRequest request = new DepositRequest();
        request.setAccountNumber("AC1000");
        request.setAmount(new BigDecimal("50.00"));
        request.setDescription("Paycheck");

        TransactionResponse response = transactionService.deposit(request);

        // The account object's balance field was mutated in place, so we
        // can assert directly on it as well as on the returned response.
        assertThat(account.getBalance()).isEqualByComparingTo("150.00");
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("150.00");
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        verify(accountRepository).save(account);
    }

    @Test
    void withdraw_decreasesBalance_whenSufficientFunds() {
        Account account = newAccount("AC1001", "200.00");
        when(accountService.getAccountEntityByNumber("AC1001")).thenReturn(account);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WithdrawRequest request = new WithdrawRequest();
        request.setAccountNumber("AC1001");
        request.setAmount(new BigDecimal("75.00"));

        TransactionResponse response = transactionService.withdraw(request);

        assertThat(account.getBalance()).isEqualByComparingTo("125.00");
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    @Test
    void withdraw_throwsInsufficientBalanceException_whenAmountExceedsBalance() {
        Account account = newAccount("AC1002", "30.00");
        when(accountService.getAccountEntityByNumber("AC1002")).thenReturn(account);

        WithdrawRequest request = new WithdrawRequest();
        request.setAccountNumber("AC1002");
        request.setAmount(new BigDecimal("100.00"));

        assertThatThrownBy(() -> transactionService.withdraw(request))
                .isInstanceOf(InsufficientBalanceException.class);

        // Balance must remain untouched, and nothing should have been
        // saved, since the withdrawal was rejected.
        assertThat(account.getBalance()).isEqualByComparingTo("30.00");
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_movesMoneyBetweenAccounts_andWritesTwoLedgerRows() {
        Account from = newAccount("AC2000", "500.00");
        Account to = newAccount("AC2001", "10.00");
        when(accountService.getAccountEntityByNumber("AC2000")).thenReturn(from);
        when(accountService.getAccountEntityByNumber("AC2001")).thenReturn(to);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("AC2000");
        request.setToAccountNumber("AC2001");
        request.setAmount(new BigDecimal("200.00"));

        TransactionResponse response = transactionService.transfer(request);

        assertThat(from.getBalance()).isEqualByComparingTo("300.00");
        assertThat(to.getBalance()).isEqualByComparingTo("210.00");
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(response.getRelatedAccountNumber()).isEqualTo("AC2001");
        // Two Transaction rows must be persisted: one per side of the transfer.
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    void transfer_throwsInsufficientBalanceException_whenSenderLacksFunds() {
        Account from = newAccount("AC3000", "10.00");
        Account to = newAccount("AC3001", "0.00");
        when(accountService.getAccountEntityByNumber("AC3000")).thenReturn(from);
        when(accountService.getAccountEntityByNumber("AC3001")).thenReturn(to);

        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("AC3000");
        request.setToAccountNumber("AC3001");
        request.setAmount(new BigDecimal("999.00"));

        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(from.getBalance()).isEqualByComparingTo("10.00");
        assertThat(to.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void transfer_throwsIllegalArgumentException_whenSameAccountBothSides() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("AC4000");
        request.setToAccountNumber("AC4000");
        request.setAmount(new BigDecimal("10.00"));

        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(IllegalArgumentException.class);

        // Should fail fast before ever looking either account up.
        verify(accountService, never()).getAccountEntityByNumber(any());
    }
}
