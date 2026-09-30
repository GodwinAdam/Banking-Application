package com.bankingapp.util;

import java.security.SecureRandom;

/**
 * Generates human-readable account numbers like "AC48213709".
 * A plain utility class (no Spring annotation, no state) - it's a pure
 * function-holder, so there's no reason to make Spring manage it as a bean.
 */
public final class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    // SecureRandom instead of java.util.Random: account numbers should not
    // be easily guessable/predictable by an attacker, even though in this
    // learning project they aren't used as a secret credential.

    private AccountNumberGenerator() {
        // Private constructor: this class only has static methods, so it
        // should never be instantiated (e.g. "new AccountNumberGenerator()").
    }

    public static String generate() {
        // Build an 8-digit number (10,000,000 - 99,999,999) and prefix it
        // with "AC" so it's visually distinguishable from a raw customer id.
        int number = 10_000_000 + RANDOM.nextInt(90_000_000);
        return "AC" + number;
    }
}
