package com.easybytes.loans.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

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
    @Min(value = 0, message = "Total loan must be >= 0")
    private Integer totalLoan;

    @Schema(description = "Amount already paid")
    @NotNull
    @Min(value = 0, message = "Amount paid must be >= 0")
    private Integer amountPaid;

    @Schema(description = "Outstanding amount")
    @NotNull
    @Min(value = 0, message = "Outstanding amount must be >= 0")
    private Integer outstandingAmount;
}
