package com.easybytes.loans.repository;

import com.easybytes.loans.audit.AuditAwareImpl;
import com.easybytes.loans.config.JpaAuditingConfig;
import com.easybytes.loans.entity.Loans;
import com.easybytes.loans.support.MySqlTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

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
        l.setTotalLoan(100_000);
        l.setAmountPaid(0);
        l.setOutstandingAmount(100_000);
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
        loan.setAmountPaid(30_000);
        loan.setOutstandingAmount(70_000);
        loansRepository.saveAndFlush(loan);

        Loans reloaded = loansRepository.findById(loan.getLoanId()).orElseThrow();
        assertThat(reloaded.getAmountPaid()).isEqualTo(30_000);
        assertThat(reloaded.getOutstandingAmount()).isEqualTo(70_000);
    }
}
