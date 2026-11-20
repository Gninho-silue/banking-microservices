package dev.silue.banking.accounts.exception;

/**
 * An Idempotency-Key was replayed with a different payload. Mapped to HTTP 409.
 */
public class IdempotencyKeyConflictException extends RuntimeException {
    public IdempotencyKeyConflictException(String idempotencyKey) {
        super("Idempotency-Key " + idempotencyKey + " was already used for a different transfer");
    }
}
