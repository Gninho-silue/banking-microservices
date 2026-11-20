package com.easybytes.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "TransferRequest", description = "Schema to request a transfer between two accounts")
public class TransferRequestDto {

    @Schema(description = "Account to debit", example = "1234567890")
    @NotNull(message = "Source account is required")
    private Long fromAccount;

    @Schema(description = "Account to credit", example = "1987654321")
    @NotNull(message = "Destination account is required")
    private Long toAccount;

    @Schema(description = "Amount to transfer (> 0, at most 2 decimals)", example = "150.00")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 digits and 2 decimals")
    private BigDecimal amount;
}
