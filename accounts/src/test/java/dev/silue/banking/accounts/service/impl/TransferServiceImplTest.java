package com.easybytes.accounts.service.impl;

import com.easybytes.accounts.dto.TransferDto;
import com.easybytes.accounts.dto.TransferRequestDto;
import com.easybytes.accounts.entity.Accounts;
import com.easybytes.accounts.entity.Transfer;
import com.easybytes.accounts.entity.TransferStatus;
import com.easybytes.accounts.exception.IdempotencyKeyConflictException;
import com.easybytes.accounts.exception.InsufficientFundsException;
import com.easybytes.accounts.exception.InvalidTransferException;
import com.easybytes.accounts.exception.ResourceNotFoundException;
import com.easybytes.accounts.repository.AccountsRepository;
import com.easybytes.accounts.repository.TransferRepository;
import com.easybytes.accounts.service.TransferResult;
import com.easybytes.accounts.service.outbox.OutboxWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    private static final long A = 1_000_000_001L;
    private static final long B = 1_000_000_002L;
    private static final String KEY = "key-123";

    @Mock
    private AccountsRepository accountsRepository;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private OutboxWriter outboxWriter;

    private TransferServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new TransferServiceImpl(accountsRepository, transferRepository, outboxWriter, transactionManager);
    }

    private static Accounts account(long number, String balance) {
        Accounts a = new Accounts();
        a.setAccountNumber(number);
        a.setBalance(new BigDecimal(balance));
        return a;
    }

    private static TransferRequestDto request(long from, long to, String amount) {
        TransferRequestDto r = new TransferRequestDto();
        r.setFromAccount(from);
        r.setToAccount(to);
        r.setAmount(new BigDecimal(amount));
        return r;
    }

    private static Transfer storedTransfer(long from, long to, String amount) {
        Transfer t = new Transfer();
        t.setId(10L);
        t.setFromAccount(from);
        t.setToAccount(to);
        t.setAmount(new BigDecimal(amount));
        t.setStatus(TransferStatus.COMPLETED);
        t.setIdempotencyKey(KEY);
        t.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return t;
    }

    private void givenLockedAccounts(Accounts... accounts) {
        for (Accounts a : accounts) {
            when(accountsRepository.findByIdForUpdate(a.getAccountNumber())).thenReturn(Optional.of(a));
        }
    }

    private void givenSaveAssignsId() {
        when(transferRepository.saveAndFlush(any(Transfer.class))).thenAnswer(inv -> {
            Transfer t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });
    }

    @Nested
    class Transfers {

        @Test
        void movesMoneyAndRecordsACompletedTransfer() {
            Accounts from = account(A, "100.00");
            Accounts to = account(B, "5.00");
            when(transferRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
            givenLockedAccounts(from, to);
            givenSaveAssignsId();

            TransferResult result = service.transfer(KEY, request(A, B, "30.5"));

            assertThat(from.getBalance()).isEqualTo(new BigDecimal("69.50"));
            assertThat(to.getBalance()).isEqualTo(new BigDecimal("35.50"));
            assertThat(result.replayed()).isFalse();
            TransferDto dto = result.transfer();
            assertThat(dto.getId()).isEqualTo(10L);
            assertThat(dto.getFromAccount()).isEqualTo(A);
            assertThat(dto.getToAccount()).isEqualTo(B);
            assertThat(dto.getAmount()).isEqualTo(new BigDecimal("30.50"));
            assertThat(dto.getStatus()).isEqualTo("COMPLETED");

            ArgumentCaptor<Transfer> saved = ArgumentCaptor.forClass(Transfer.class);
            verify(transferRepository).saveAndFlush(saved.capture());
            assertThat(saved.getValue().getIdempotencyKey()).isEqualTo(KEY);
            assertThat(saved.getValue().getStatus()).isEqualTo(TransferStatus.COMPLETED);
        }

        @Test
        void writesTheTransferCompletedOutboxEventAfterSavingTheTransfer() {
            when(transferRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
            givenLockedAccounts(account(A, "100.00"), account(B, "0.00"));
            givenSaveAssignsId();

            service.transfer(KEY, request(A, B, "1"));

            InOrder order = inOrder(transferRepository, outboxWriter);
            order.verify(transferRepository).saveAndFlush(any(Transfer.class));
            ArgumentCaptor<Transfer> event = ArgumentCaptor.forClass(Transfer.class);
            order.verify(outboxWriter).transferCompleted(event.capture());
            assertThat(event.getValue().getId()).isEqualTo(10L);
        }

        @Test
        void locksBothAccountsInAscendingOrderEvenWhenSourceIsTheHigherNumber() {
            Accounts high = account(B, "100.00");
            Accounts low = account(A, "0.00");
            when(transferRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
            givenLockedAccounts(high, low);
            givenSaveAssignsId();

            service.transfer(KEY, request(B, A, "10"));

            InOrder order = inOrder(accountsRepository);
            order.verify(accountsRepository).findByIdForUpdate(A);
            order.verify(accountsRepository).findByIdForUpdate(B);
            assertThat(low.getBalance()).isEqualTo(new BigDecimal("10.00"));
        }

        @Test
        void rejectsInsufficientFundsWithoutRecordingAnything() {
            Accounts from = account(A, "10.00");
            Accounts to = account(B, "0.00");
            when(transferRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
            givenLockedAccounts(from, to);

            assertThatThrownBy(() -> service.transfer(KEY, request(A, B, "10.01")))
                    .isInstanceOf(InsufficientFundsException.class);

            assertThat(from.getBalance()).isEqualTo(new BigDecimal("10.00"));
            assertThat(to.getBalance()).isEqualTo(new BigDecimal("0.00"));
            verify(transferRepository, never()).saveAndFlush(any());
            verifyNoInteractions(outboxWriter);
        }

        @Test
        void rejectsUnknownAccount() {
            when(transferRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
            when(accountsRepository.findByIdForUpdate(A)).thenReturn(Optional.of(account(A, "50.00")));
            when(accountsRepository.findByIdForUpdate(B)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.transfer(KEY, request(A, B, "1")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account not found with the given input data accountNumber: " + B);
            verify(transferRepository, never()).saveAndFlush(any());
            verifyNoInteractions(outboxWriter);
        }

        @Test
        void rejectsInvalidRequestBeforeTouchingTheDatabase() {
            assertThatThrownBy(() -> service.transfer(KEY, request(A, A, "1")))
                    .isInstanceOf(InvalidTransferException.class);
            assertThatThrownBy(() -> service.transfer(KEY, request(A, B, "0")))
                    .isInstanceOf(InvalidTransferException.class);

            verifyNoInteractions(accountsRepository, transferRepository);
        }

        @Test
        void requiresAnIdempotencyKey() {
            assertThatThrownBy(() -> service.transfer("  ", request(A, B, "1")))
                    .isInstanceOf(InvalidTransferException.class)
                    .hasMessage("Idempotency-Key header is required");
            verifyNoInteractions(accountsRepository, transferRepository);
        }
    }

    @Nested
    class Idempotency {

        @Test
        void replayWithSameKeyReturnsStoredTransferWithoutMovingMoney() {
            when(transferRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(storedTransfer(A, B, "30.50")));

            TransferResult result = service.transfer(KEY, request(A, B, "30.5"));

            assertThat(result.replayed()).isTrue();
            assertThat(result.transfer().getId()).isEqualTo(10L);
            verifyNoInteractions(accountsRepository);
            verify(transferRepository, never()).saveAndFlush(any());
            verifyNoInteractions(outboxWriter);
        }

        @Test
        void replayDetectedOnceLocksAreHeldAlsoMovesNoMoney() {
            Accounts from = account(A, "100.00");
            Accounts to = account(B, "0.00");
            // not visible before locking, committed by a concurrent request in the meantime
            when(transferRepository.findByIdempotencyKey(KEY))
                    .thenReturn(Optional.empty(), Optional.of(storedTransfer(A, B, "30.50")));
            givenLockedAccounts(from, to);

            TransferResult result = service.transfer(KEY, request(A, B, "30.50"));

            assertThat(result.replayed()).isTrue();
            assertThat(from.getBalance()).isEqualTo(new BigDecimal("100.00"));
            verify(transferRepository, never()).saveAndFlush(any());
            verifyNoInteractions(outboxWriter);
        }

        @Test
        void sameKeyWithDifferentPayloadIsAConflict() {
            when(transferRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(storedTransfer(A, B, "30.50")));

            assertThatThrownBy(() -> service.transfer(KEY, request(A, B, "99")))
                    .isInstanceOf(IdempotencyKeyConflictException.class)
                    .hasMessage("Idempotency-Key key-123 was already used for a different transfer");
            verifyNoInteractions(accountsRepository);
        }

        @Test
        void losingTheRaceOnTheUniqueKeyReturnsTheWinnersTransfer() {
            when(transferRepository.findByIdempotencyKey(KEY))
                    .thenReturn(Optional.empty(), Optional.empty(), Optional.of(storedTransfer(A, B, "30.50")));
            givenLockedAccounts(account(A, "100.00"), account(B, "0.00"));
            when(transferRepository.saveAndFlush(any()))
                    .thenThrow(new DataIntegrityViolationException("Duplicate entry 'key-123' for key 'uk_transfers_idempotency_key'"));

            TransferResult result = service.transfer(KEY, request(A, B, "30.50"));

            assertThat(result.replayed()).isTrue();
            assertThat(result.transfer().getId()).isEqualTo(10L);
            verify(transactionManager).rollback(any());
        }
    }

    @Nested
    class Queries {

        @Test
        void getTransferById() {
            when(transferRepository.findById(10L)).thenReturn(Optional.of(storedTransfer(A, B, "1.00")));

            assertThat(service.getTransfer(10L).getAmount()).isEqualTo(new BigDecimal("1.00"));
        }

        @Test
        void unknownTransferIsNotFound() {
            when(transferRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTransfer(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transfer not found with the given input data id: 99");
        }

        @Test
        void listsTransfersWhereAccountIsSourceOrDestination() {
            when(accountsRepository.existsById(A)).thenReturn(true);
            when(transferRepository.findByFromAccountOrToAccountOrderByCreatedAtDescIdDesc(A, A))
                    .thenReturn(List.of(storedTransfer(A, B, "1.00"), storedTransfer(B, A, "2.00")));

            List<TransferDto> transfers = service.getTransfersForAccount(A);

            assertThat(transfers).extracting(TransferDto::getAmount)
                    .containsExactly(new BigDecimal("1.00"), new BigDecimal("2.00"));
        }

        @Test
        void listingTransfersOfUnknownAccountIsNotFound() {
            when(accountsRepository.existsById(A)).thenReturn(false);

            assertThatThrownBy(() -> service.getTransfersForAccount(A))
                    .isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(transferRepository);
        }
    }
}
