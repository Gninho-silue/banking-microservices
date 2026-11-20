package com.easybytes.accounts.service.outbox;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload of the {@code TransferCompleted} event published on the {@code transfer-completed} topic.
 * Delivery is at-least-once: consumers should de-duplicate on {@code transferId}.
 */
public record TransferCompletedEvent(
        Long transferId,
        Long fromAccount,
        Long toAccount,
        BigDecimal amount,
        LocalDateTime completedAt
) {
}
