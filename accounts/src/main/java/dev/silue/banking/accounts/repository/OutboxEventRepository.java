package com.easybytes.accounts.repository;

import com.easybytes.accounts.entity.OutboxEvent;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Oldest unpublished events, row-locked with {@code FOR UPDATE SKIP LOCKED} (lock timeout -2):
     * several accounts instances can publish concurrently without sending the same event twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select e from OutboxEvent e where e.publishedAt is null order by e.id")
    List<OutboxEvent> findUnpublished(Limit limit);

    default List<OutboxEvent> lockUnpublishedBatch(int batchSize) {
        return findUnpublished(Limit.of(batchSize));
    }

    long countByPublishedAtIsNull();
}
