package com.easybytes.loans.service.schedule;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One monthly installment: {@code payment = interest + principal}; {@code remaining} is the capital still
 * due after this payment. All amounts have scale 2.
 */
public record Installment(
        int n,
        LocalDate dueDate,
        BigDecimal payment,
        BigDecimal interest,
        BigDecimal principal,
        BigDecimal remaining
) {
}
