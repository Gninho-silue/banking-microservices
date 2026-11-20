package com.easybytes.loans.service.schedule;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param monthlyPayment constant installment (the last one may differ by the rounding adjustment)
 * @param totalPayment   sum of all payments ({@code principal + totalInterest})
 * @param totalInterest  sum of all interests
 * @param installments   installments 1..n in order
 */
public record AmortizationSchedule(
        BigDecimal monthlyPayment,
        BigDecimal totalPayment,
        BigDecimal totalInterest,
        List<Installment> installments
) {
}
