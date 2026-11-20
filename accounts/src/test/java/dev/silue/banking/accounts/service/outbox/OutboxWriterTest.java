package com.easybytes.accounts.service.outbox;

import com.easybytes.accounts.entity.OutboxEvent;
import com.easybytes.accounts.entity.Transfer;
import com.easybytes.accounts.entity.TransferStatus;
import com.easybytes.accounts.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxWriterTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void storesATransferCompletedEventWithTheTransferAsJsonPayload() {
        Transfer transfer = new Transfer();
        transfer.setId(42L);
        transfer.setFromAccount(1_000_000_001L);
        transfer.setToAccount(1_000_000_002L);
        transfer.setAmount(new BigDecimal("150.00"));
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setIdempotencyKey("k-1");
        transfer.setCreatedAt(LocalDateTime.of(2026, 1, 2, 3, 4, 5));

        new OutboxWriter(outboxEventRepository, jsonMapper).transferCompleted(transfer);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent event = captor.getValue();
        assertThat(event.getType()).isEqualTo("TransferCompleted");
        assertThat(event.getAggregateId()).isEqualTo("42");
        assertThat(event.getPublishedAt()).isNull();

        JsonNode payload = jsonMapper.readTree(event.getPayload());
        assertThat(payload.get("transferId").asLong()).isEqualTo(42L);
        assertThat(payload.get("fromAccount").asLong()).isEqualTo(1_000_000_001L);
        assertThat(payload.get("toAccount").asLong()).isEqualTo(1_000_000_002L);
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("150.00");
        assertThat(payload.get("completedAt").asString()).isEqualTo("2026-01-02T03:04:05");
    }
}
