package com.easybytes.accounts.repository;

import com.easybytes.accounts.entity.Accounts;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


public interface AccountsRepository extends JpaRepository<Accounts, Long> {

    Optional<Accounts> findByCustomerId(Long customerId);

    @Transactional
    @Modifying
    void deleteByCustomerId(Long customerId);

    boolean existsByAccountNumber(Long customerId);

    /**
     * Loads the account with a row lock ({@code SELECT ... FOR UPDATE}); must run inside a transaction.
     * Concurrent writers on the same account wait until the lock holder commits.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Accounts a where a.accountNumber = :accountNumber")
    Optional<Accounts> findByIdForUpdate(@Param("accountNumber") Long accountNumber);
}
