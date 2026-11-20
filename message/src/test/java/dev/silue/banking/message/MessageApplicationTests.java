package com.easybytes.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Boots the service with the in-memory Spring Cloud Stream test binder (no Kafka) and checks the
 * bindings: a JSON TransferCompleted event published by accounts' outbox reaches transferNotification.
 */
@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class MessageApplicationTests {

    @Autowired
    private InputDestination input;

    @Test
    void transferCompletedEventFromKafkaTopicIsNotified(CapturedOutput output) {
        String json = """
                {"transferId":7,"fromAccount":1000000001,"toAccount":1000000002,
                 "amount":12.34,"completedAt":"2026-01-02T03:04:05"}
                """;

        input.send(MessageBuilder.withPayload(json.getBytes())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .build(), "transfer-completed");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(output).contains("Transfer 7 completed").contains("account 1000000002 credited of 12.34"));
    }
}
