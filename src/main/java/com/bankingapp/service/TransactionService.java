package com.bankingapp.service;

import com.bankingapp.dto.DepositRequest;
import com.bankingapp.dto.TransactionResponse;
import com.bankingapp.dto.TransferRequest;
import com.bankingapp.dto.WithdrawRequest;

import java.util.List;

public interface TransactionService {

    TransactionResponse deposit(DepositRequest request);

    TransactionResponse withdraw(WithdrawRequest request);

    /**
     * A transfer produces TWO ledger rows (one TRANSFER_OUT on the source
     * account, one TRANSFER_IN on the destination account). We return the
     * TRANSFER_OUT row, from the caller's point of view "my side of what
     * just happened".
     */
    TransactionResponse transfer(TransferRequest request);

    List<TransactionResponse> getTransactionHistory(String accountNumber);
}
