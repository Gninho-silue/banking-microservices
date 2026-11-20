package dev.silue.banking.accounts.transfer;

import dev.silue.banking.accounts.dto.TransferRequestDto;
import dev.silue.banking.accounts.entity.Accounts;
import dev.silue.banking.accounts.repository.AccountsRepository;
import dev.silue.banking.accounts.repository.OutboxEventRepository;
import dev.silue.banking.accounts.service.IAccountsService;
import dev.silue.banking.accounts.service.ITransferService;
import dev.silue.banking.accounts.service.TransferResult;
import dev.silue.banking.accounts.support.MySqlTestContainerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Full outbox path with real infrastructure: transfer committed in MySQL -> outbox row ->
 * scheduled OutboxPublisher -> Kafka topic "transfer-completed" -> row marked published.
 */
@SpringBootTest(properties = {
        "outbox.publisher.enabled=true",
        "outbox.publisher.fixed-delay=200"
})
@ActiveProfiles("test")
@Import(MySqlTestContainerConfig.class)
@Testcontainers
class OutboxKafkaIntegrationTest {

    private static final String TOPIC = "transfer-completed";

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.1.1");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private ITransferService transferService;
    @Autowired
    private IAccountsService accountsService;
    @Autowired
    private AccountsRepository accountsRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private long newAccount(long number, String deposit) {
        Accounts account = new Accounts();
        account.setAccountNumber(number);
        account.setCustomerId(1L);
        account.setAccountType("Savings");
        account.setBranchAddress("123 Main Street, New York");
        accountsRepository.save(account);
        accountsService.deposit(number, new BigDecimal(deposit));
        return number;
    }

    @Test
    void completedTransferIsPublishedToKafkaAndMarkedPublished() {
        long a = newAccount(1_700_000_001L, "100.00");
        long b = newAccount(1_700_000_002L, "1.00");
        TransferRequestDto request = new TransferRequestDto();
        request.setFromAccount(a);
        request.setToAccount(b);
        request.setAmount(new BigDecimal("12.34"));

        TransferResult result = transferService.transfer(UUID.randomUUID().toString(), request);
        long transferId = result.transfer().getId();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(outboxEventRepository.countByPublishedAtIsNull()).isZero());

        JsonNode event = consumeEventFor(transferId);
        assertThat(event.get("fromAccount").asLong()).isEqualTo(a);
        assertThat(event.get("toAccount").asLong()).isEqualTo(b);
        assertThat(event.get("amount").decimalValue()).isEqualByComparingTo("12.34");
    }

    private JsonNode consumeEventFor(long transferId) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "outbox-it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
            consumer.subscribe(List.of(TOPIC));
            List<JsonNode> received = new ArrayList<>();
            await().atMost(Duration.ofSeconds(30)).until(() -> {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    received.add(jsonMapper.readTree(record.value()));
                }
                return received.stream().anyMatch(e -> e.get("transferId").asLong() == transferId);
            });
            return received.stream().filter(e -> e.get("transferId").asLong() == transferId).findFirst().orElseThrow();
        }
    }
}
