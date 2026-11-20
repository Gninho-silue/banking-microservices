package dev.silue.banking.loans.exception;

public class LoanAlreadyExistsException extends RuntimeException {
    public LoanAlreadyExistsException(String message) {
        super(message);
    }
}
