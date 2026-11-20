package dev.silue.banking.loans.mapper;

import dev.silue.banking.loans.dto.LoansDto;
import dev.silue.banking.loans.entity.Loans;

public class LoansMapper {

    public static LoansDto toLoansDto(Loans loans, LoansDto dto) {
        dto.setMobileNumber(loans.getMobileNumber());
        dto.setLoanNumber(loans.getLoanNumber());
        dto.setLoanType(loans.getLoanType());
        dto.setTotalLoan(loans.getTotalLoan());
        dto.setAmountPaid(loans.getAmountPaid());
        dto.setOutstandingAmount(loans.getOutstandingAmount());
        dto.setAnnualInterestRate(loans.getAnnualInterestRate());
        dto.setTermMonths(loans.getTermMonths());
        dto.setStartDate(loans.getStartDate());
        return dto;
    }

    public static Loans toLoans(LoansDto dto, Loans entity) {
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setLoanNumber(dto.getLoanNumber());
        entity.setLoanType(dto.getLoanType());
        entity.setTotalLoan(dto.getTotalLoan());
        entity.setAmountPaid(dto.getAmountPaid());
        entity.setOutstandingAmount(dto.getOutstandingAmount());
        // terms are optional on update (older clients do not send them); startDate is never changed
        if (dto.getAnnualInterestRate() != null) {
            entity.setAnnualInterestRate(dto.getAnnualInterestRate());
        }
        if (dto.getTermMonths() != null) {
            entity.setTermMonths(dto.getTermMonths());
        }
        return entity;
    }
}
