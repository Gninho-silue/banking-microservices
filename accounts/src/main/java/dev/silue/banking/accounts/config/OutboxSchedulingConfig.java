package dev.silue.banking.accounts.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on the scheduled outbox publisher. Disabled in the "test" profile, where tests
 * drive {@code OutboxPublisher#publishPending()} themselves (except the Kafka integration test).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxSchedulingConfig {
}
