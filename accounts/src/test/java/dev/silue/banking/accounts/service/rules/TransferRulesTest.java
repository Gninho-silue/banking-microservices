package dev.silue.banking.accounts.service.rules;

import dev.silue.banking.accounts.exception.InvalidTransferException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferRulesTest {

    private static final Long FROM = 1_000_000_001L;
    private static final Long TO = 1_000_000_002L;

    @Test
    void acceptsValidTransferAndNormalizesAmountToScaleTwo() {
        assertThat(TransferRules.validateTransfer(FROM, TO, new BigDecimal("12.5")))
                .isEqualTo(new BigDecimal("12.50"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "-0.01"})
    void rejectsNonPositiveAmounts(String amount) {
        assertThatThrownBy(() -> TransferRules.validateTransfer(FROM, TO, new BigDecimal(amount)))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Amount must be greater than 0");
    }

    @Test
    void rejectsMissingAmount() {
        assertThatThrownBy(() -> TransferRules.validateTransfer(FROM, TO, null))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Amount must be greater than 0");
    }

    @Test
    void rejectsMoreThanTwoDecimals() {
        assertThatThrownBy(() -> TransferRules.validateTransfer(FROM, TO, new BigDecimal("1.005")))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Amount must have at most 2 decimals");
    }

    @Test
    void rejectsTransferToSameAccount() {
        assertThatThrownBy(() -> TransferRules.validateTransfer(FROM, FROM, BigDecimal.TEN))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Source and destination accounts must be different");
    }

    @Test
    void rejectsMissingAccounts() {
        assertThatThrownBy(() -> TransferRules.validateTransfer(null, TO, BigDecimal.TEN))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Source and destination accounts are required");
        assertThatThrownBy(() -> TransferRules.validateTransfer(FROM, null, BigDecimal.TEN))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Source and destination accounts are required");
    }

    @Test
    void depositAmountFollowsTheSameAmountRules() {
        assertThat(TransferRules.validateAmount(new BigDecimal("7"))).isEqualTo(new BigDecimal("7.00"));
        assertThatThrownBy(() -> TransferRules.validateAmount(new BigDecimal("-3")))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    void lockOrderIsAscendingByAccountNumberWhateverTheDirection() {
        assertThat(TransferRules.lockOrder(TO, FROM)).containsExactly(FROM, TO);
        assertThat(TransferRules.lockOrder(FROM, TO)).containsExactly(FROM, TO);
    }
}
