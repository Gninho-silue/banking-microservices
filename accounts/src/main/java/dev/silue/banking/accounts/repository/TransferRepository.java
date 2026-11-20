package dev.silue.banking.accounts.repository;

import dev.silue.banking.accounts.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    /** Transfers where the account is either the source or the destination, newest first. */
    List<Transfer> findByFromAccountOrToAccountOrderByCreatedAtDescIdDesc(Long fromAccount, Long toAccount);
}
