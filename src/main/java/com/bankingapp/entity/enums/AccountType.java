package com.bankingapp.entity.enums;

/**
 * The two kinds of bank account this system supports.
 * Using an enum instead of a raw String prevents typos like "SAVING" vs
 * "SAVINGS" from ever reaching the database.
 */
public enum AccountType {
    SAVINGS,
    CURRENT
}
