package com.easybytes.loans.service.schedule;

import com.easybytes.loans.exception.InvalidLoanTermsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmortizationCalculatorTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 15);

    private static AmortizationSchedule compute(String principal, String rate, int months) {
        return AmortizationCalculator.compute(new BigDecimal(principal), new BigDecimal(rate), months, START);
    }

    private static BigDecimal sum(List<Installment> installments, java.util.function.Function<Installment, BigDecimal> f) {
        return installments.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void referenceValue100000At5PercentOver12MonthsGives8560_75() {
        AmortizationSchedule schedule = compute("100000.00", "0.0500", 12);

        assertThat(schedule.monthlyPayment()).isEqualTo(new BigDecimal("8560.75"));
        Installment first = schedule.installments().getFirst();
        assertThat(first.n()).isEqualTo(1);
        assertThat(first.payment()).isEqualTo(new BigDecimal("8560.75"));
        assertThat(first.interest()).isEqualTo(new BigDecimal("416.67"));
        assertThat(first.principal()).isEqualTo(new BigDecimal("8144.08"));
        assertThat(first.remaining()).isEqualTo(new BigDecimal("91855.92"));
    }

    @Test
    void scheduleHasOneInstallmentPerMonth() {
        assertThat(compute("100000.00", "0.0500", 12).installments())
                .hasSize(12)
                .extracting(Installment::n)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

    @ParameterizedTest
    @CsvSource({
            "100000.00, 0.0500, 12",
            "200000.00, 0.0350, 240",
            "1234.56,   0.1999, 7",
            "50000.00,  0.0000, 36",
            "999.99,    0.0125, 1"
    })
    void principalsSumToTheCapitalAndTheLastRemainingIsExactlyZero(String principal, String rate, int months) {
        AmortizationSchedule schedule = compute(principal, rate, months);
        List<Installment> installments = schedule.installments();

        assertThat(sum(installments, Installment::principal)).isEqualTo(new BigDecimal(principal).setScale(2));
        assertThat(installments.getLast().remaining()).isEqualTo(new BigDecimal("0.00"));
        assertThat(installments).allSatisfy(i -> {
            assertThat(i.payment().scale()).isEqualTo(2);
            assertThat(i.interest().scale()).isEqualTo(2);
            assertThat(i.principal().scale()).isEqualTo(2);
            assertThat(i.remaining().signum()).isGreaterThanOrEqualTo(0);
            assertThat(i.payment()).isEqualTo(i.interest().add(i.principal()));
        });
    }

    @Test
    void totalsAreConsistentWithTheInstallments() {
        AmortizationSchedule schedule = compute("100000.00", "0.0500", 12);

        assertThat(schedule.totalInterest()).isEqualTo(sum(schedule.installments(), Installment::interest));
        assertThat(schedule.totalPayment()).isEqualTo(sum(schedule.installments(), Installment::payment));
        assertThat(schedule.totalPayment()).isEqualTo(new BigDecimal("100000.00").add(schedule.totalInterest()));
    }

    @Test
    void lastInstallmentAbsorbsTheRoundingDifference() {
        AmortizationSchedule schedule = compute("1000.00", "0.0000", 3);

        assertThat(schedule.monthlyPayment()).isEqualTo(new BigDecimal("333.33"));
        assertThat(schedule.installments()).extracting(Installment::payment)
                .containsExactly(new BigDecimal("333.33"), new BigDecimal("333.33"), new BigDecimal("333.34"));
    }

    @Test
    void zeroRateMeansNoInterest() {
        AmortizationSchedule schedule = compute("1200.00", "0", 12);

        assertThat(schedule.totalInterest()).isEqualTo(new BigDecimal("0.00"));
        assertThat(schedule.installments()).allSatisfy(i -> {
            assertThat(i.interest()).isEqualTo(new BigDecimal("0.00"));
            assertThat(i.payment()).isEqualTo(new BigDecimal("100.00"));
        });
    }

    @Test
    void dueDatesAreMonthlyFromTheStartDateClampedToMonthEnd() {
        AmortizationSchedule schedule = AmortizationCalculator.compute(
                new BigDecimal("300.00"), new BigDecimal("0.0500"), 3, LocalDate.of(2026, 1, 31));

        assertThat(schedule.installments()).extracting(Installment::dueDate).containsExactly(
                LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30));
    }

    @Test
    void rejectsNonPositivePrincipal() {
        assertThatThrownBy(() -> compute("0", "0.05", 12))
                .isInstanceOf(InvalidLoanTermsException.class)
                .hasMessage("Principal must be greater than 0");
    }

    @Test
    void rejectsNegativeRate() {
        assertThatThrownBy(() -> compute("1000", "-0.01", 12))
                .isInstanceOf(InvalidLoanTermsException.class)
                .hasMessage("Annual interest rate must be >= 0");
    }

    @Test
    void rejectsNonPositiveTerm() {
        assertThatThrownBy(() -> compute("1000", "0.05", 0))
                .isInstanceOf(InvalidLoanTermsException.class)
                .hasMessage("Term must be at least 1 month");
    }

    @Test
    void rejectsMissingValues() {
        assertThatThrownBy(() -> AmortizationCalculator.compute(null, BigDecimal.ONE, 12, START))
                .isInstanceOf(InvalidLoanTermsException.class);
        assertThatThrownBy(() -> AmortizationCalculator.compute(BigDecimal.TEN, null, 12, START))
                .isInstanceOf(InvalidLoanTermsException.class);
        assertThatThrownBy(() -> AmortizationCalculator.compute(BigDecimal.TEN, BigDecimal.ONE, 12, null))
                .isInstanceOf(InvalidLoanTermsException.class);
    }
}
