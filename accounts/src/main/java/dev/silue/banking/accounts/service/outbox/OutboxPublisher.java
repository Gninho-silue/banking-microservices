package dev.silue.banking.accounts.service.outbox;

import dev.silue.banking.accounts.entity.OutboxEvent;
import dev.silue.banking.accounts.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Relays outbox rows to Kafka. Runs every {@code outbox.publisher.fixed-delay} ms when
 * {@code outbox.publisher.enabled} is true (see {@code OutboxSchedulingConfig}).
 * <p>
 * Events are sent in id order and the batch stops at the first failure, so an event is never
 * published before an older one. The Kafka producer is synchronous (see application.yml): a row is
 * marked published only once the broker acknowledged it. A crash between the send and the commit
 * re-sends the event: delivery is at-least-once.
 */
@Component
public class OutboxPublisher {

    public static final String BINDING = "transferCompleted-out-0";

    private final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final StreamBridge streamBridge;
    private final int batchSize;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, StreamBridge streamBridge,
                           @Value("${outbox.publisher.batch-size:100}") int batchSize) {
        this.outboxEventRepository = outboxEventRepository;
        this.streamBridge = streamBridge;
        this.batchSize = batchSize;
    }

    /**
     * @return the number of events published in this run
     */
    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay:1000}")
    @Transactional
    public int publishPending() {
        List<OutboxEvent> batch = outboxEventRepository.lockUnpublishedBatch(batchSize);
        int published = 0;
        for (OutboxEvent event : batch) {
            if (!send(event)) {
                break;
            }
            event.setPublishedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
            published++;
        }
        if (published > 0) {
            logger.info("Published {} outbox event(s)", published);
        }
        return published;
    }

    private boolean send(OutboxEvent event) {
        Message<String> message = MessageBuilder.withPayload(event.getPayload())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .setHeader("eventType", event.getType())
                .setHeader("aggregateId", event.getAggregateId())
                .build();
        try {
            if (streamBridge.send(BINDING, message)) {
                return true;
            }
            logger.warn("Outbox event {} was not accepted by the binder, will retry", event.getId());
        } catch (RuntimeException e) {
            logger.warn("Outbox event {} could not be published, will retry: {}", event.getId(), e.getMessage());
        }
        return false;
    }
}
