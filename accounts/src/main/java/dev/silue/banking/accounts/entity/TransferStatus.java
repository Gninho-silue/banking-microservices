package dev.silue.banking.accounts.entity;

/**
 * Only successful transfers are recorded: a rejected request (validation, insufficient funds)
 * leaves no row, so replaying its Idempotency-Key evaluates it again.
 */
public enum TransferStatus {
    COMPLETED
}
