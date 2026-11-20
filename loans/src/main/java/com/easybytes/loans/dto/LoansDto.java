package com.easybytes.loans.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(name = "Loan", description = "Schema to hold Loan information")
public class LoansDto {

    @Schema(description = "Customer Mobile Number", example = "0612345678")
    @NotBlank(message = "Customer mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must 10 digits")
    private String mobileNumber;

    @Schema(description = "Loan Number", example = "7098123412341234")
    @Digits(integer = 19, fraction = 0, message = "Loan Number must be numeric")
    @Size(min = 12, max = 19, message = "Loan Number must be between 12 and 19 digits")
    private String loanNumber;

    @Schema(description = "Type of the loan", example = "HOME")
    @NotBlank(message = "Loan Type is required")
    @Pattern(regexp = "HOME|PERSONAL|AUTO", message = "Loan Type must be HOME, PERSONAL or AUTO")
    private String loanType;

    @Schema(description = "Total loan amount")
    @NotNull
    @DecimalMin(value = "0.00", message = "Total loan must be >= 0")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 digits and 2 decimals")
    private BigDecimal totalLoan;

    @Schema(description = "Amount already paid")
    @NotNull
    @DecimalMin(value = "0.00", message = "Amount paid must be >= 0")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 digits and 2 decimals")
    private BigDecimal amountPaid;

    @Schema(description = "Outstanding amount")
    @NotNull
    @DecimalMin(value = "0.00", message = "Outstanding amount must be >= 0")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 digits and 2 decimals")
    private BigDecimal outstandingAmount;

    @Schema(description = "Nominal annual interest rate as a fraction (0.0500 = 5 %); unchanged on update when omitted",
            example = "0.0500")
    @DecimalMin(value = "0.0000", message = "Annual interest rate must be >= 0")
    @DecimalMax(value = "1.0000", message = "Annual interest rate must be <= 1")
    @Digits(integer = 1, fraction = 4, message = "Annual interest rate must have at most 4 decimals")
    private BigDecimal annualInterestRate;

    @Schema(description = "Loan term in months; unchanged on update when omitted", example = "60")
    @Min(value = 1, message = "Term must be at least 1 month")
    @Max(value = 600, message = "Term must be at most 600 months")
    private Integer termMonths;

    @Schema(description = "First day of the loan (set at creation)", example = "2026-01-15",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDate startDate;
}
