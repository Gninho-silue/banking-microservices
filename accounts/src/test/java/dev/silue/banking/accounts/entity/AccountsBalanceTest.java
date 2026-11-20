package dev.silue.banking.accounts.entity;

import dev.silue.banking.accounts.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountsBalanceTest {

    private static Accounts accountWithBalance(String balance) {
        Accounts account = new Accounts();
        account.setAccountNumber(1_000_000_001L);
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    @Test
    void newAccountStartsWithZeroBalanceAtScaleTwo() {
        assertThat(new Accounts().getBalance()).isEqualByComparingTo("0").hasScaleOf(2);
    }

    @Test
    void creditAddsAmount() {
        Accounts account = accountWithBalance("100.00");

        account.credit(new BigDecimal("25.50"));

        assertThat(account.getBalance()).isEqualTo(new BigDecimal("125.50"));
    }

    @Test
    void debitSubtractsAmount() {
        Accounts account = accountWithBalance("100.00");

        account.debit(new BigDecimal("40.25"));

        assertThat(account.getBalance()).isEqualTo(new BigDecimal("59.75"));
    }

    @Test
    void debitOfWholeBalanceLeavesZero() {
        Accounts account = accountWithBalance("10.00");

        account.debit(new BigDecimal("10.00"));

        assertThat(account.getBalance()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void debitBeyondBalanceIsRejectedAndLeavesBalanceUntouched() {
        Accounts account = accountWithBalance("10.00");

        assertThatThrownBy(() -> account.debit(new BigDecimal("10.01")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("Insufficient funds on account 1000000001");
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void balanceIsAlwaysKeptAtScaleTwo() {
        Accounts account = accountWithBalance("0");

        account.credit(new BigDecimal("5"));

        assertThat(account.getBalance()).isEqualTo(new BigDecimal("5.00"));
    }
}
