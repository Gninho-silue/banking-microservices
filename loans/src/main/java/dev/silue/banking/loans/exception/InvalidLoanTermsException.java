package dev.silue.banking.loans.exception;

/**
 * Loan terms that cannot be amortized (non-positive principal or term, negative rate). Mapped to HTTP 400.
 */
public class InvalidLoanTermsException extends RuntimeException {
    public InvalidLoanTermsException(String message) {
        super(message);
    }
}
