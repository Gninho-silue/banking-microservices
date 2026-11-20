package dev.silue.banking.loans.service.schedule;

import dev.silue.banking.loans.exception.InvalidLoanTermsException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Constant-payment (French) amortization, pure Java (no Spring, no I/O).
 * <pre>
 *   r = annual rate / 12
 *   M = P * r / (1 - (1 + r)^-n)        (M = P / n when r = 0)
 * </pre>
 * Intermediate maths use {@link MathContext#DECIMAL64}; every amount is rounded to 2 decimals with
 * {@link RoundingMode#HALF_EVEN}. Each month: {@code interest = round(remaining * r)},
 * {@code principal = M - interest}. The last installment repays exactly the remaining capital
 * (absorbing the accumulated rounding difference), so the schedule always ends at 0.00.
 */
public final class AmortizationCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private AmortizationCalculator() {
    }

    public static AmortizationSchedule compute(BigDecimal principal, BigDecimal annualRate, int termMonths,
                                               LocalDate startDate) {
        validate(principal, annualRate, termMonths, startDate);

        BigDecimal capital = money(principal);
        BigDecimal monthlyRate = annualRate.divide(MONTHS_PER_YEAR, MC);
        BigDecimal monthlyPayment = monthlyPayment(capital, monthlyRate, termMonths);

        List<Installment> installments = new ArrayList<>(termMonths);
        BigDecimal remaining = capital;
        BigDecimal totalInterest = money(BigDecimal.ZERO);
        BigDecimal totalPayment = money(BigDecimal.ZERO);
        for (int n = 1; n <= termMonths; n++) {
            BigDecimal interest = money(remaining.multiply(monthlyRate, MC));
            BigDecimal principalPart = n == termMonths
                    ? remaining                                   // last one absorbs the rounding difference
                    : monthlyPayment.subtract(interest).min(remaining);
            BigDecimal payment = principalPart.add(interest);
            remaining = remaining.subtract(principalPart);

            installments.add(new Installment(n, startDate.plusMonths(n), payment, interest, principalPart, remaining));
            totalInterest = totalInterest.add(interest);
            totalPayment = totalPayment.add(payment);
        }
        return new AmortizationSchedule(monthlyPayment, totalPayment, totalInterest, List.copyOf(installments));
    }

    private static BigDecimal monthlyPayment(BigDecimal capital, BigDecimal monthlyRate, int termMonths) {
        if (monthlyRate.signum() == 0) {
            return capital.divide(BigDecimal.valueOf(termMonths), SCALE, ROUNDING);
        }
        BigDecimal discount = BigDecimal.ONE.subtract(BigDecimal.ONE.add(monthlyRate).pow(-termMonths, MC), MC);
        return money(capital.multiply(monthlyRate, MC).divide(discount, MC));
    }

    private static void validate(BigDecimal principal, BigDecimal annualRate, int termMonths, LocalDate startDate) {
        if (principal == null || principal.signum() <= 0) {
            throw new InvalidLoanTermsException("Principal must be greater than 0");
        }
        if (annualRate == null || annualRate.signum() < 0) {
            throw new InvalidLoanTermsException("Annual interest rate must be >= 0");
        }
        if (termMonths < 1) {
            throw new InvalidLoanTermsException("Term must be at least 1 month");
        }
        if (startDate == null) {
            throw new InvalidLoanTermsException("Start date is required");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }
}
