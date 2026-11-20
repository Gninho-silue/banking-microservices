package dev.silue.banking.loans.repository;

import dev.silue.banking.loans.entity.Loans;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoansRepository extends JpaRepository<Loans, Long> {
    Optional<Loans> findByMobileNumber(String mobileNumber);
    boolean existsByLoanNumber(String loanNumber);
    Optional<Loans> findByLoanNumber(String loanNumber);
}
