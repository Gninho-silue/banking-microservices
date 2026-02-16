package com.easybytes.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(name = "Account", description = "Schema to hold Account information")
public class AccountsDto {


    @NotBlank(message = "Account Number is required")
    @Digits(integer = 12, fraction = 0, message = "Account Number must be numeric")
    @Min(value = 1000000000L, message = "Account Number must have at least 10 digits")
    @Schema(
            description = "Easy Bank's Account Number"
    )
    private Long accountNumber;

    @NotBlank(message = "Account Type is required")
    @Pattern(
            regexp = "SAVINGS|CURRENT|CHECKING",
            message = "Account Type must be SAVINGS, CURRENT or CHECKING"
    )
    @Schema(
            description = "Type of the account",
            example = "SAVINGS"
    )
    private String accountType;

    @NotBlank(message = "Branch Address is required")
    @Size(min = 5, max = 100, message = "Branch Address must be between 5 and 100 characters")
    @Schema(
            description = "Address of the bank branch",
            example = "123 Main Street, City"

    )
    private String branchAddress;
}
