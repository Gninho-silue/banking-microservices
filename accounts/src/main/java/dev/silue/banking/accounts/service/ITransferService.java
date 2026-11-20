package com.easybytes.accounts.service;

import com.easybytes.accounts.dto.TransferDto;
import com.easybytes.accounts.dto.TransferRequestDto;

import java.util.List;

public interface ITransferService {

    /**
     * Moves money between two accounts, exactly once per Idempotency-Key.
     * Replaying a key with the same payload returns the stored transfer without moving money.
     */
    TransferResult transfer(String idempotencyKey, TransferRequestDto request);

    TransferDto getTransfer(Long id);

    /** Transfers where the account is the source or the destination, newest first. */
    List<TransferDto> getTransfersForAccount(Long accountNumber);
}
