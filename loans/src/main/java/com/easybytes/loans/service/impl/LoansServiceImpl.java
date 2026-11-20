package com.easybytes.loans.service.impl;

import com.easybytes.loans.constants.LoansConstants;
import com.easybytes.loans.dto.LoansDto;
import com.easybytes.loans.entity.Loans;
import com.easybytes.loans.exception.LoanAlreadyExistsException;
import com.easybytes.loans.exception.ResourceNotFoundException;
import com.easybytes.loans.mapper.LoansMapper;
import com.easybytes.loans.repository.LoansRepository;
import com.easybytes.loans.dto.LoanScheduleDto;
import com.easybytes.loans.service.ILoansService;
import com.easybytes.loans.service.schedule.AmortizationCalculator;
import com.easybytes.loans.service.schedule.AmortizationSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class LoansServiceImpl implements ILoansService {

    private final LoansRepository loansRepository;
    private final Clock clock;

    @Override
    public void createLoan(String mobileNumber) {
        Optional<Loans> existing = loansRepository.findByMobileNumber(mobileNumber);
        if (existing.isPresent()) {
            throw new LoanAlreadyExistsException("Loan already exists for mobile: " + mobileNumber);
        }

        Loans loan = new Loans();
        loan.setMobileNumber(mobileNumber);
        loan.setLoanType(LoansConstants.PERSONAL_LOAN); // default type; adjust as needed
        loan.setTotalLoan(LoansConstants.NEW_LOAN_LIMIT);
        loan.setAmountPaid(BigDecimal.ZERO.setScale(2));
        loan.setOutstandingAmount(LoansConstants.NEW_LOAN_LIMIT);
        loan.setLoanNumber(generateUniqueLoanNumber());
        loan.setAnnualInterestRate(LoansConstants.DEFAULT_ANNUAL_INTEREST_RATE);
        loan.setTermMonths(LoansConstants.DEFAULT_TERM_MONTHS);
        loan.setStartDate(LocalDate.now(clock));
        loansRepository.save(loan);
    }

    @Override
    public LoansDto fetchLoan(String mobileNumber) {
        Loans loans = loansRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loans not found for mobile: " + mobileNumber));
        return LoansMapper.toLoansDto(loans, new LoansDto());
    }

    @Override
    public boolean updateLoan(LoansDto loansDto) {
        Loans loans = loansRepository.findByLoanNumber(loansDto.getLoanNumber()).orElseThrow(
                ()-> new ResourceNotFoundException("Loan not found with loan number:"+ loansDto.getLoanNumber())
        );
        LoansMapper.toLoans(loansDto, loans);
        loansRepository.save(loans);
        return true;
    }

    @Override
    public LoanScheduleDto getSchedule(String mobileNumber) {
        Loans loan = loansRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loans not found for mobile: " + mobileNumber));
        AmortizationSchedule schedule = AmortizationCalculator.compute(loan.getTotalLoan(),
                loan.getAnnualInterestRate(), loan.getTermMonths(), loan.getStartDate());

        LoanScheduleDto dto = new LoanScheduleDto();
        dto.setLoanNumber(loan.getLoanNumber());
        dto.setMobileNumber(loan.getMobileNumber());
        dto.setPrincipal(loan.getTotalLoan());
        dto.setAnnualInterestRate(loan.getAnnualInterestRate());
        dto.setTermMonths(loan.getTermMonths());
        dto.setStartDate(loan.getStartDate());
        dto.setMonthlyPayment(schedule.monthlyPayment());
        dto.setTotalInterest(schedule.totalInterest());
        dto.setTotalPayment(schedule.totalPayment());
        dto.setInstallments(schedule.installments());
        return dto;
    }

    @Override
    public boolean deleteLoan(String mobileNumber) {
      Loans loans = loansRepository.findByMobileNumber(mobileNumber).orElseThrow(
              ()-> new ResourceNotFoundException("Loan not found for mobile: " + mobileNumber)
      );
      loansRepository.deleteById(loans.getLoanId());
      return true;
    }

    private String generateUniqueLoanNumber() {
        String number;
        int attempts = 0;
        do {
            number = generate16DigitNumber();
            attempts++;
            if (attempts > 10) {
                number = generate19DigitNumber();
            }
        } while (loansRepository.existsByLoanNumber(number));
        return number;
    }

    private String generate16DigitNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String generate19DigitNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(19);
        for (int i = 0; i < 19; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
