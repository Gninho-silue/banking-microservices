package com.easybytes.accounts.transfer;

import com.easybytes.accounts.dto.TransferRequestDto;
import com.easybytes.accounts.entity.Accounts;
import com.easybytes.accounts.exception.InsufficientFundsException;
import com.easybytes.accounts.repository.AccountsRepository;
import com.easybytes.accounts.repository.TransferRepository;
import com.easybytes.accounts.service.IAccountsService;
import com.easybytes.accounts.service.ITransferService;
import com.easybytes.accounts.service.TransferResult;
import com.easybytes.accounts.support.MySqlTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end transfer behaviour against a real MySQL (row locks, UNIQUE constraint, DECIMAL columns).
 * No @Transactional here: every call commits, exactly like in production.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({MySqlTestContainerConfig.class, TestChannelBinderConfiguration.class})
class TransferIntegrationTest {

    private static final AtomicLong NEXT_ACCOUNT = new AtomicLong(1_800_000_000L);

    @Autowired
    private ITransferService transferService;
    @Autowired
    private IAccountsService accountsService;
    @Autowired
    private AccountsRepository accountsRepository;
    @Autowired
    private TransferRepository transferRepository;

    private long newAccount(String initialDeposit) {
        Accounts account = new Accounts();
        account.setAccountNumber(NEXT_ACCOUNT.incrementAndGet());
        account.setCustomerId(1L);
        account.setAccountType("Savings");
        account.setBranchAddress("123 Main Street, New York");
        accountsRepository.save(account);
        if (new BigDecimal(initialDeposit).signum() > 0) {
            accountsService.deposit(account.getAccountNumber(), new BigDecimal(initialDeposit));
        }
        return account.getAccountNumber();
    }

    private BigDecimal balanceOf(long accountNumber) {
        return accountsRepository.findById(accountNumber).orElseThrow().getBalance();
    }

    private static TransferRequestDto request(long from, long to, String amount) {
        TransferRequestDto r = new TransferRequestDto();
        r.setFromAccount(from);
        r.setToAccount(to);
        r.setAmount(new BigDecimal(amount));
        return r;
    }

    private static <T> List<Future<T>> runConcurrently(int threads, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return task.call();
            }));
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).as("no deadlock / hang").isTrue();
        return futures;
    }

    @Test
    void depositThenTransferIsPersistedWithExactDecimals() {
        long a = newAccount("100.10");
        long b = newAccount("0");

        TransferResult result = transferService.transfer(UUID.randomUUID().toString(), request(a, b, "33.33"));

        assertThat(balanceOf(a)).isEqualTo(new BigDecimal("66.77"));
        assertThat(balanceOf(b)).isEqualTo(new BigDecimal("33.33"));
        assertThat(transferRepository.findById(result.transfer().getId())).isPresent();
        assertThat(transferService.getTransfersForAccount(b)).hasSize(1);
    }

    @Test
    void replayingTheSameKeyNeverDebitsTwice() {
        long a = newAccount("100.00");
        long b = newAccount("0");
        String key = UUID.randomUUID().toString();

        TransferResult first = transferService.transfer(key, request(a, b, "40.00"));
        TransferResult second = transferService.transfer(key, request(a, b, "40.00"));

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.transfer()).isEqualTo(first.transfer());
        assertThat(balanceOf(a)).isEqualTo(new BigDecimal("60.00"));
        assertThat(balanceOf(b)).isEqualTo(new BigDecimal("40.00"));
    }

    @Test
    void twentyParallelTransfersNeverOverdrawTheAccount() throws Exception {
        long a = newAccount("100.00");
        long b = newAccount("0");

        List<Future<Boolean>> results = runConcurrently(20, () -> {
            try {
                transferService.transfer(UUID.randomUUID().toString(), request(a, b, "10.00"));
                return true;
            } catch (InsufficientFundsException e) {
                return false;
            }
        });

        long succeeded = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) {
                succeeded++;
            }
        }
        assertThat(succeeded).isEqualTo(10);
        assertThat(balanceOf(a)).isEqualTo(new BigDecimal("0.00"));
        assertThat(balanceOf(b)).isEqualTo(new BigDecimal("100.00"));
        assertThat(transferService.getTransfersForAccount(a)).hasSize(10);
    }

    @Test
    void twentyParallelRequestsWithTheSameKeyMoveMoneyOnce() throws Exception {
        long a = newAccount("100.00");
        long b = newAccount("0");
        String key = UUID.randomUUID().toString();

        List<Future<TransferResult>> results = runConcurrently(20, () -> transferService.transfer(key, request(a, b, "25.00")));

        long originals = 0;
        Long transferId = null;
        for (Future<TransferResult> f : results) {
            TransferResult r = f.get();
            originals += r.replayed() ? 0 : 1;
            transferId = transferId == null ? r.transfer().getId() : transferId;
            assertThat(r.transfer().getId()).isEqualTo(transferId);
        }
        assertThat(originals).isEqualTo(1);
        assertThat(balanceOf(a)).isEqualTo(new BigDecimal("75.00"));
        assertThat(balanceOf(b)).isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    void oppositeTransfersInParallelDoNotDeadlockAndConserveMoney() throws Exception {
        long a = newAccount("500.00");
        long b = newAccount("500.00");
        AtomicLong counter = new AtomicLong();

        List<Future<Boolean>> results = runConcurrently(20, () -> {
            boolean aToB = counter.incrementAndGet() % 2 == 0;
            transferService.transfer(UUID.randomUUID().toString(),
                    aToB ? request(a, b, "7.00") : request(b, a, "3.00"));
            return true;
        });
        for (Future<Boolean> f : results) {
            assertThat(f.get()).isTrue();
        }

        // 10 x (A->B 7.00) and 10 x (B->A 3.00)
        assertThat(balanceOf(a)).isEqualTo(new BigDecimal("460.00"));
        assertThat(balanceOf(b)).isEqualTo(new BigDecimal("540.00"));
    }
}
