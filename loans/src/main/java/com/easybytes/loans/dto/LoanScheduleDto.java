package com.easybytes.loans.dto;

import com.easybytes.loans.service.schedule.Installment;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(name = "LoanSchedule", description = "Constant-payment amortization schedule of a loan")
public class LoanScheduleDto {

    @Schema(description = "Loan Number", example = "7098123412341234")
    private String loanNumber;

    @Schema(description = "Customer Mobile Number", example = "0612345678")
    private String mobileNumber;

    @Schema(description = "Amortized capital (total loan)", example = "100000.00")
    private BigDecimal principal;

    @Schema(description = "Nominal annual interest rate as a fraction", example = "0.0500")
    private BigDecimal annualInterestRate;

    @Schema(description = "Number of monthly installments", example = "12")
    private Integer termMonths;

    @Schema(description = "Loan start date; installment n is due n months later", example = "2026-01-15")
    private LocalDate startDate;

    @Schema(description = "Constant monthly payment (the last one absorbs rounding)", example = "8560.75")
    private BigDecimal monthlyPayment;

    @Schema(description = "Sum of all interests", example = "2729.01")
    private BigDecimal totalInterest;

    @Schema(description = "Sum of all payments = principal + total interest", example = "102729.01")
    private BigDecimal totalPayment;

    @ArraySchema(schema = @Schema(implementation = Installment.class,
            description = "n, dueDate, payment, interest, principal, remaining"))
    private List<Installment> installments;
}
