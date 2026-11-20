package dev.silue.banking.accounts.mapper;

import dev.silue.banking.accounts.dto.TransferDto;
import dev.silue.banking.accounts.entity.Transfer;

public class TransferMapper {

    private TransferMapper() {
    }

    public static TransferDto toTransferDto(Transfer transfer) {
        TransferDto dto = new TransferDto();
        dto.setId(transfer.getId());
        dto.setFromAccount(transfer.getFromAccount());
        dto.setToAccount(transfer.getToAccount());
        dto.setAmount(transfer.getAmount());
        dto.setStatus(transfer.getStatus().name());
        dto.setCreatedAt(transfer.getCreatedAt());
        return dto;
    }
}
