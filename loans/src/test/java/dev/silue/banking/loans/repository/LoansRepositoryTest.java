package dev.silue.banking.loans.repository;

import dev.silue.banking.loans.audit.AuditAwareImpl;
import dev.silue.banking.loans.config.JpaAuditingConfig;
import dev.silue.banking.loans.entity.Loans;
import dev.silue.banking.loans.support.MySqlTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests against a real MySQL (Testcontainers), with the schema
 * created by {@code src/main/resources/schema.sql}.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({MySqlTestContainerConfig.class, JpaAuditingConfig.class, AuditAwareImpl.class})
class LoansRepositoryTest {

    @Autowired
    private LoansRepository loansRepository;

    private Loans saveLoan(String mobile, String loanNumber) {
        Loans l = new Loans();
        l.setMobileNumber(mobile);
        l.setLoanNumber(loanNumber);
        l.setLoanType("PERSONAL");
        l.setTotalLoan(new BigDecimal("100000.00"));
        l.setAmountPaid(new BigDecimal("0.00"));
        l.setOutstandingAmount(new BigDecimal("100000.00"));
        l.setAnnualInterestRate(new BigDecimal("0.0500"));
        l.setTermMonths(60);
        l.setStartDate(LocalDate.of(2026, 1, 31));
        return loansRepository.saveAndFlush(l);
    }

    @Test
    void savedLoanGetsIdAndAuditColumns() {
        Loans saved = saveLoan("0611111111", "1111222233334444");

        assertThat(saved.getLoanId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("LOANS_SERVICE");
    }

    @Test
    void findsByMobileNumber() {
        saveLoan("0622222222", "2222333344445555");

        assertThat(loansRepository.findByMobileNumber("0622222222"))
                .get().extracting(Loans::getLoanNumber).isEqualTo("2222333344445555");
        assertThat(loansRepository.findByMobileNumber("0699999999")).isEmpty();
    }

    @Test
    void findsAndChecksExistenceByLoanNumber() {
        saveLoan("0633333333", "3333444455556666");

        assertThat(loansRepository.existsByLoanNumber("3333444455556666")).isTrue();
        assertThat(loansRepository.existsByLoanNumber("0000000000000000")).isFalse();
        assertThat(loansRepository.findByLoanNumber("3333444455556666"))
                .get().extracting(Loans::getMobileNumber).isEqualTo("0633333333");
    }

    @Test
    void persistsUpdatedAmounts() {
        Loans loan = saveLoan("0644444444", "4444555566667777");
        loan.setAmountPaid(new BigDecimal("30000.00"));
        loan.setOutstandingAmount(new BigDecimal("70000.00"));
        loansRepository.saveAndFlush(loan);

        Loans reloaded = loansRepository.findById(loan.getLoanId()).orElseThrow();
        assertThat(reloaded.getAmountPaid()).isEqualTo(new BigDecimal("30000.00"));
        assertThat(reloaded.getOutstandingAmount()).isEqualTo(new BigDecimal("70000.00"));
    }

    @Test
    void persistsLoanTermsWithTheirSqlTypes() {
        Loans loan = saveLoan("0655555555", "5555666677778888");
        loan.setAnnualInterestRate(new BigDecimal("0.0325"));
        loan.setTermMonths(240);
        loansRepository.saveAndFlush(loan);

        Loans reloaded = loansRepository.findById(loan.getLoanId()).orElseThrow();
        assertThat(reloaded.getAnnualInterestRate()).isEqualTo(new BigDecimal("0.0325"));
        assertThat(reloaded.getTermMonths()).isEqualTo(240);
        assertThat(reloaded.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 31));
    }
}
