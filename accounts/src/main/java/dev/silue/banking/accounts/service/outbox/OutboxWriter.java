package dev.silue.banking.accounts.service.outbox;

import dev.silue.banking.accounts.entity.OutboxEvent;
import dev.silue.banking.accounts.entity.Transfer;
import dev.silue.banking.accounts.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Records integration events in the outbox table. Must be called inside the business
 * transaction, so the event exists if and only if the change was committed.
 */
@Component
public class OutboxWriter {

    public static final String TRANSFER_COMPLETED = "TransferCompleted";

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public OutboxWriter(OutboxEventRepository outboxEventRepository, JsonMapper jsonMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
    }

    public void transferCompleted(Transfer transfer) {
        TransferCompletedEvent payload = new TransferCompletedEvent(transfer.getId(), transfer.getFromAccount(),
                transfer.getToAccount(), transfer.getAmount(), transfer.getCreatedAt());
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(String.valueOf(transfer.getId()));
        event.setType(TRANSFER_COMPLETED);
        event.setPayload(jsonMapper.writeValueAsString(payload));
        outboxEventRepository.save(event);
    }
}
