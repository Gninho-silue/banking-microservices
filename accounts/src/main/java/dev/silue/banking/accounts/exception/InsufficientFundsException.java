package com.easybytes.accounts.exception;

/**
 * Raised when a debit would make an account balance negative. Mapped to HTTP 422.
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long accountNumber) {
        super("Insufficient funds on account " + accountNumber);
    }
}
