package com.bankingapp.entity.enums;

/**
 * Every row in the "transactions" table (our ledger) is tagged with one of
 * these. DEPOSIT/WITHDRAWAL are single-account operations; TRANSFER_OUT and
 * TRANSFER_IN are written as a matching *pair* (one per account) whenever a
 * transfer happens, so each account's history reads correctly on its own.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_OUT,
    TRANSFER_IN
}
