package dev.silue.banking.accounts.exception;

/**
 * Raised when a transfer or deposit request breaks a business rule
 * (non-positive amount, same source and destination...). Mapped to HTTP 400.
 */
public class InvalidTransferException extends RuntimeException {
    public InvalidTransferException(String message) {
        super(message);
    }
}
