package com.easybytes.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "DepositRequest", description = "Schema to credit an account")
public class DepositRequestDto {

    @Schema(description = "Amount to deposit (> 0, at most 2 decimals)", example = "500.00")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 digits and 2 decimals")
    private BigDecimal amount;
}
