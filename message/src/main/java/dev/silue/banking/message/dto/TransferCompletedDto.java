package com.easybytes.message.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransferCompleted event published by the accounts service (transactional outbox) on the
 * {@code transfer-completed} topic. Delivery is at-least-once: {@code transferId} identifies duplicates.
 */
public record TransferCompletedDto(
        Long transferId,
        Long fromAccount,
        Long toAccount,
        BigDecimal amount,
        LocalDateTime completedAt
) {
}
