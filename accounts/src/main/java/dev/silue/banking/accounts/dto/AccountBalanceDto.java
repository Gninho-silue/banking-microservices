package dev.silue.banking.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AccountBalance", description = "Schema to hold an account balance")
public class AccountBalanceDto {

    @Schema(description = "Account number", example = "1234567890")
    private Long accountNumber;

    @Schema(description = "Current balance", example = "500.00")
    private BigDecimal balance;
}
