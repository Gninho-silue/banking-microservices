package com.easybytes.message.function;

import com.easybytes.message.dto.AccountsMsgDto;
import com.easybytes.message.dto.TransferCompletedDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class MessageFunctionsTest {

    private final MessageFunctions functions = new MessageFunctions();
    private final AccountsMsgDto account = new AccountsMsgDto(1_000_000_001L, "Jane", "jane@example.com", "0612345678");

    @Test
    void emailPassesTheMessageThrough(CapturedOutput output) {
        assertThat(functions.email().apply(account)).isSameAs(account);
        assertThat(output).contains("Sending email to jane@example.com");
    }

    @Test
    void smsReturnsTheAccountNumberToAcknowledge(CapturedOutput output) {
        assertThat(functions.sms().apply(account)).isEqualTo(1_000_000_001L);
        assertThat(output).contains("Sending sms to 0612345678");
    }

    @Test
    void transferNotificationLogsBothSidesOfTheTransfer(CapturedOutput output) {
        TransferCompletedDto event = new TransferCompletedDto(42L, 1_000_000_001L, 1_000_000_002L,
                new BigDecimal("150.00"), LocalDateTime.of(2026, 1, 2, 3, 4, 5));

        functions.transferNotification().accept(event);

        assertThat(output)
                .contains("Transfer 42 completed")
                .contains("account 1000000001 debited of 150.00")
                .contains("account 1000000002 credited of 150.00");
    }
}
