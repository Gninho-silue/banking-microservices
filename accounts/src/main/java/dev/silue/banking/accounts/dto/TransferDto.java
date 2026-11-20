package dev.silue.banking.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(name = "Transfer", description = "Schema to hold a completed transfer")
public class TransferDto {

    @Schema(description = "Transfer id", example = "1")
    private Long id;

    @Schema(description = "Debited account", example = "1234567890")
    private Long fromAccount;

    @Schema(description = "Credited account", example = "1987654321")
    private Long toAccount;

    @Schema(description = "Transferred amount", example = "150.00")
    private BigDecimal amount;

    @Schema(description = "Transfer status", example = "COMPLETED")
    private String status;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
}
