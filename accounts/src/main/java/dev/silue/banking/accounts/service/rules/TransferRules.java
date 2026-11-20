package dev.silue.banking.accounts.service.rules;

import dev.silue.banking.accounts.exception.InvalidTransferException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure business rules for money movements (no Spring, no I/O).
 */
public final class TransferRules {

    /** Monetary amounts are stored as DECIMAL(19,2). */
    public static final int MONEY_SCALE = 2;

    private TransferRules() {
    }

    /**
     * Validates a transfer request and returns the amount normalized to scale 2.
     */
    public static BigDecimal validateTransfer(Long fromAccount, Long toAccount, BigDecimal amount) {
        if (fromAccount == null || toAccount == null) {
            throw new InvalidTransferException("Source and destination accounts are required");
        }
        if (fromAccount.equals(toAccount)) {
            throw new InvalidTransferException("Source and destination accounts must be different");
        }
        return validateAmount(amount);
    }

    /**
     * Validates a monetary amount (strictly positive, at most 2 decimals) and normalizes it to scale 2.
     */
    public static BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransferException("Amount must be greater than 0");
        }
        if (amount.stripTrailingZeros().scale() > MONEY_SCALE) {
            throw new InvalidTransferException("Amount must have at most 2 decimals");
        }
        return toMoney(amount);
    }

    /**
     * Accounts are always locked in ascending account-number order so that two opposite
     * transfers (A→B and B→A) can never wait on each other (no deadlock).
     */
    public static List<Long> lockOrder(Long fromAccount, Long toAccount) {
        return fromAccount < toAccount ? List.of(fromAccount, toAccount) : List.of(toAccount, fromAccount);
    }

    public static BigDecimal toMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }
}
