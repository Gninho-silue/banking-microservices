package dev.silue.banking.accounts.service;

import dev.silue.banking.accounts.dto.TransferDto;

/**
 * @param transfer the transfer (new, or the one previously stored for the Idempotency-Key)
 * @param replayed {@code true} when no money moved because the key had already been processed
 */
public record TransferResult(TransferDto transfer, boolean replayed) {
}
