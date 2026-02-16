package com.easybytes.loans.mapper;

import com.easybytes.loans.dto.LoansDto;
import com.easybytes.loans.entity.Loans;

public class LoansMapper {

    public static LoansDto toLoansDto(Loans loans, LoansDto dto) {
        dto.setMobileNumber(loans.getMobileNumber());
        dto.setLoanNumber(loans.getLoanNumber());
        dto.setLoanType(loans.getLoanType());
        dto.setTotalLoan(loans.getTotalLoan());
        dto.setAmountPaid(loans.getAmountPaid());
        dto.setOutstandingAmount(loans.getOutstandingAmount());
        return dto;
    }

    public static Loans toLoans(LoansDto dto, Loans entity) {
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setLoanNumber(dto.getLoanNumber());
        entity.setLoanType(dto.getLoanType());
        entity.setTotalLoan(dto.getTotalLoan());
        entity.setAmountPaid(dto.getAmountPaid());
        entity.setOutstandingAmount(dto.getOutstandingAmount());
        return entity;
    }
}
