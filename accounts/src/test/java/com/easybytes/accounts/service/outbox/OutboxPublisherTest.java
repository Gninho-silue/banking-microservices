package com.easybytes.accounts.service.outbox;

import com.easybytes.accounts.entity.OutboxEvent;
import com.easybytes.accounts.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private StreamBridge streamBridge;

    private OutboxPublisher publisher() {
        return new OutboxPublisher(outboxEventRepository, streamBridge, 50);
    }

    private static OutboxEvent event(long id) {
        OutboxEvent e = new OutboxEvent();
        e.setId(id);
        e.setAggregateId(String.valueOf(id * 10));
        e.setType("TransferCompleted");
        e.setPayload("{\"transferId\":" + id * 10 + "}");
        return e;
    }

    @Test
    void publishesPendingEventsInOrderAndMarksThemPublished() {
        OutboxEvent first = event(1);
        OutboxEvent second = event(2);
        when(outboxEventRepository.lockUnpublishedBatch(50)).thenReturn(List.of(first, second));
        when(streamBridge.send(eq(OutboxPublisher.BINDING), any(Message.class))).thenReturn(true);

        int published = publisher().publishPending();

        assertThat(published).isEqualTo(2);
        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(second.getPublishedAt()).isNotNull();

        ArgumentCaptor<Message<String>> sent = ArgumentCaptor.forClass(Message.class);
        InOrder order = inOrder(streamBridge);
        order.verify(streamBridge, times(2)).send(eq(OutboxPublisher.BINDING), sent.capture());
        Message<String> message = sent.getAllValues().get(0);
        assertThat(message.getPayload()).isEqualTo("{\"transferId\":10}");
        assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeTypeUtils.APPLICATION_JSON_VALUE);
        assertThat(message.getHeaders().get("eventType")).isEqualTo("TransferCompleted");
        assertThat(message.getHeaders().get("aggregateId")).isEqualTo("10");
    }

    @Test
    void stopsAtTheFirstFailedSendToPreserveOrder() {
        OutboxEvent first = event(1);
        OutboxEvent second = event(2);
        when(outboxEventRepository.lockUnpublishedBatch(50)).thenReturn(List.of(first, second));
        when(streamBridge.send(eq(OutboxPublisher.BINDING), any(Message.class))).thenReturn(false);

        int published = publisher().publishPending();

        assertThat(published).isZero();
        assertThat(first.getPublishedAt()).isNull();
        assertThat(second.getPublishedAt()).isNull();
        verify(streamBridge, times(1)).send(eq(OutboxPublisher.BINDING), any(Message.class));
    }

    @Test
    void brokerErrorKeepsEarlierEventsPublishedAndLaterOnesPending() {
        OutboxEvent first = event(1);
        OutboxEvent second = event(2);
        OutboxEvent third = event(3);
        when(outboxEventRepository.lockUnpublishedBatch(50)).thenReturn(List.of(first, second, third));
        when(streamBridge.send(eq(OutboxPublisher.BINDING), any(Message.class)))
                .thenReturn(true)
                .thenThrow(new IllegalStateException("broker unavailable"));

        int published = publisher().publishPending();

        assertThat(published).isEqualTo(1);
        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(second.getPublishedAt()).isNull();
        assertThat(third.getPublishedAt()).isNull();
        verify(streamBridge, times(2)).send(eq(OutboxPublisher.BINDING), any(Message.class));
    }

    @Test
    void doesNothingWhenThereIsNothingToPublish() {
        when(outboxEventRepository.lockUnpublishedBatch(anyInt())).thenReturn(List.of());

        assertThat(publisher().publishPending()).isZero();

        verifyNoInteractions(streamBridge);
        verify(outboxEventRepository, never()).save(any());
    }
}
