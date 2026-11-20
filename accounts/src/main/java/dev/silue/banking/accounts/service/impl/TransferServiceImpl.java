package dev.silue.banking.accounts.service.impl;

import dev.silue.banking.accounts.dto.TransferDto;
import dev.silue.banking.accounts.dto.TransferRequestDto;
import dev.silue.banking.accounts.entity.Accounts;
import dev.silue.banking.accounts.entity.Transfer;
import dev.silue.banking.accounts.entity.TransferStatus;
import dev.silue.banking.accounts.exception.IdempotencyKeyConflictException;
import dev.silue.banking.accounts.exception.InvalidTransferException;
import dev.silue.banking.accounts.exception.ResourceNotFoundException;
import dev.silue.banking.accounts.mapper.TransferMapper;
import dev.silue.banking.accounts.repository.AccountsRepository;
import dev.silue.banking.accounts.repository.TransferRepository;
import dev.silue.banking.accounts.service.ITransferService;
import dev.silue.banking.accounts.service.TransferResult;
import dev.silue.banking.accounts.service.outbox.OutboxWriter;
import dev.silue.banking.accounts.service.rules.TransferRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Idempotent, concurrency-safe transfers.
 * <ol>
 *     <li>Fast path: a key that was already processed is answered from the {@code transfers} table.</li>
 *     <li>Otherwise, in one transaction: lock both accounts ({@code SELECT ... FOR UPDATE}) in ascending
 *     account-number order (no deadlock), re-check the key now that concurrent requests are serialized,
 *     debit/credit, and insert the transfer.</li>
 *     <li>Safety net: the UNIQUE constraint on {@code idempotency_key} rolls back a duplicate that slipped
 *     through; the stored transfer is then returned as a replay.</li>
 * </ol>
 */
@Service
public class TransferServiceImpl implements ITransferService {

    private final Logger logger = LoggerFactory.getLogger(TransferServiceImpl.class);

    private final AccountsRepository accountsRepository;
    private final TransferRepository transferRepository;
    private final OutboxWriter outboxWriter;
    private final TransactionTemplate transactionTemplate;

    public TransferServiceImpl(AccountsRepository accountsRepository,
                               TransferRepository transferRepository,
                               OutboxWriter outboxWriter,
                               PlatformTransactionManager transactionManager) {
        this.accountsRepository = accountsRepository;
        this.transferRepository = transferRepository;
        this.outboxWriter = outboxWriter;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public TransferResult transfer(String idempotencyKey, TransferRequestDto request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidTransferException("Idempotency-Key header is required");
        }
        BigDecimal amount = TransferRules.validateTransfer(request.getFromAccount(), request.getToAccount(),
                request.getAmount());

        Optional<Transfer> alreadyProcessed = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (alreadyProcessed.isPresent()) {
            return replay(alreadyProcessed.get(), idempotencyKey, request, amount);
        }
        try {
            return transactionTemplate.execute(status -> execute(idempotencyKey, request, amount));
        } catch (DataIntegrityViolationException e) {
            logger.info("Concurrent request already processed Idempotency-Key {}", idempotencyKey);
            return transferRepository.findByIdempotencyKey(idempotencyKey)
                    .map(existing -> replay(existing, idempotencyKey, request, amount))
                    .orElseThrow(() -> e);
        }
    }

    private TransferResult execute(String idempotencyKey, TransferRequestDto request, BigDecimal amount) {
        Map<Long, Accounts> locked = new HashMap<>();
        for (Long accountNumber : TransferRules.lockOrder(request.getFromAccount(), request.getToAccount())) {
            locked.put(accountNumber, accountsRepository.findByIdForUpdate(accountNumber).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "accountNumber", accountNumber.toString())));
        }

        // A concurrent request with the same key may have committed while we were waiting for the locks.
        Optional<Transfer> alreadyProcessed = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (alreadyProcessed.isPresent()) {
            return replay(alreadyProcessed.get(), idempotencyKey, request, amount);
        }

        locked.get(request.getFromAccount()).debit(amount);
        locked.get(request.getToAccount()).credit(amount);

        Transfer transfer = new Transfer();
        transfer.setFromAccount(request.getFromAccount());
        transfer.setToAccount(request.getToAccount());
        transfer.setAmount(amount);
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setIdempotencyKey(idempotencyKey);
        Transfer saved = transferRepository.saveAndFlush(transfer);
        outboxWriter.transferCompleted(saved); // same transaction: event exists iff the transfer committed
        logger.info("Transfer {} completed: {} -> {} ({})", saved.getId(), saved.getFromAccount(),
                saved.getToAccount(), saved.getAmount());
        return new TransferResult(TransferMapper.toTransferDto(saved), false);
    }

    private TransferResult replay(Transfer existing, String idempotencyKey, TransferRequestDto request,
                                  BigDecimal amount) {
        boolean samePayload = existing.getFromAccount().equals(request.getFromAccount())
                && existing.getToAccount().equals(request.getToAccount())
                && existing.getAmount().compareTo(amount) == 0;
        if (!samePayload) {
            throw new IdempotencyKeyConflictException(idempotencyKey);
        }
        return new TransferResult(TransferMapper.toTransferDto(existing), true);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferDto getTransfer(Long id) {
        return transferRepository.findById(id)
                .map(TransferMapper::toTransferDto)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", "id", String.valueOf(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferDto> getTransfersForAccount(Long accountNumber) {
        if (!accountsRepository.existsById(accountNumber)) {
            throw new ResourceNotFoundException("Account", "accountNumber", String.valueOf(accountNumber));
        }
        return transferRepository.findByFromAccountOrToAccountOrderByCreatedAtDescIdDesc(accountNumber, accountNumber)
                .stream()
                .map(TransferMapper::toTransferDto)
                .toList();
    }
}
