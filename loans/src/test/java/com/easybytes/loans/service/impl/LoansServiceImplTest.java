package com.easybytes.loans.service.impl;

import com.easybytes.loans.constants.LoansConstants;
import com.easybytes.loans.dto.LoansDto;
import com.easybytes.loans.entity.Loans;
import com.easybytes.loans.exception.LoanAlreadyExistsException;
import com.easybytes.loans.exception.ResourceNotFoundException;
import com.easybytes.loans.repository.LoansRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoansServiceImplTest {

    private static final String MOBILE = "0612345678";

    @Mock
    private LoansRepository loansRepository;

    @InjectMocks
    private LoansServiceImpl service;

    private static Loans loan(long id, String loanNumber) {
        Loans l = new Loans();
        l.setLoanId(id);
        l.setMobileNumber(MOBILE);
        l.setLoanNumber(loanNumber);
        l.setLoanType(LoansConstants.PERSONAL_LOAN);
        l.setTotalLoan(100_000);
        l.setAmountPaid(0);
        l.setOutstandingAmount(100_000);
        return l;
    }

    @Nested
    class CreateLoan {

        @Test
        void createsDefaultPersonalLoanWith16DigitNumber() {
            when(loansRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());
            when(loansRepository.existsByLoanNumber(anyString())).thenReturn(false);

            service.createLoan(MOBILE);

            ArgumentCaptor<Loans> captor = ArgumentCaptor.forClass(Loans.class);
            verify(loansRepository).save(captor.capture());
            Loans saved = captor.getValue();
            assertThat(saved.getMobileNumber()).isEqualTo(MOBILE);
            assertThat(saved.getLoanType()).isEqualTo(LoansConstants.PERSONAL_LOAN);
            assertThat(saved.getTotalLoan()).isEqualTo(LoansConstants.NEW_LOAN_LIMIT);
            assertThat(saved.getAmountPaid()).isZero();
            assertThat(saved.getOutstandingAmount()).isEqualTo(LoansConstants.NEW_LOAN_LIMIT);
            assertThat(saved.getLoanNumber()).matches("\\d{16}");
        }

        @Test
        void retriesWhenGeneratedNumberAlreadyExists() {
            when(loansRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());
            when(loansRepository.existsByLoanNumber(anyString())).thenReturn(true, true, false);

            service.createLoan(MOBILE);

            verify(loansRepository, times(3)).existsByLoanNumber(anyString());
        }

        @Test
        void fallsBackTo19DigitNumberAfterTenCollisions() {
            when(loansRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());
            // 10 collisions on 16-digit numbers, then the 11th attempt (19 digits) is free
            when(loansRepository.existsByLoanNumber(anyString()))
                    .thenReturn(true, true, true, true, true, true, true, true, true, true, false);

            service.createLoan(MOBILE);

            ArgumentCaptor<Loans> captor = ArgumentCaptor.forClass(Loans.class);
            verify(loansRepository).save(captor.capture());
            assertThat(captor.getValue().getLoanNumber()).matches("\\d{19}");
        }

        @Test
        void rejectsSecondLoanForSameMobile() {
            when(loansRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(loan(1L, "1234567890123456")));

            assertThatThrownBy(() -> service.createLoan(MOBILE))
                    .isInstanceOf(LoanAlreadyExistsException.class)
                    .hasMessage("Loan already exists for mobile: " + MOBILE);
            verify(loansRepository, never()).save(any());
        }
    }

    @Nested
    class FetchLoan {

        @Test
        void returnsLoanDetails() {
            when(loansRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(loan(1L, "1234567890123456")));

            LoansDto dto = service.fetchLoan(MOBILE);

            assertThat(dto.getMobileNumber()).isEqualTo(MOBILE);
            assertThat(dto.getLoanNumber()).isEqualTo("1234567890123456");
            assertThat(dto.getLoanType()).isEqualTo("PERSONAL");
            assertThat(dto.getTotalLoan()).isEqualTo(100_000);
            assertThat(dto.getAmountPaid()).isZero();
            assertThat(dto.getOutstandingAmount()).isEqualTo(100_000);
        }

        @Test
        void throwsWhenNoLoanForMobile() {
            when(loansRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.fetchLoan(MOBILE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Loans not found for mobile: " + MOBILE);
        }
    }

    @Nested
    class UpdateLoan {

        @Test
        void updatesAmountsOfExistingLoan() {
            Loans existing = loan(1L, "1234567890123456");
            when(loansRepository.findByLoanNumber("1234567890123456")).thenReturn(Optional.of(existing));
            LoansDto dto = new LoansDto();
            dto.setMobileNumber(MOBILE);
            dto.setLoanNumber("1234567890123456");
            dto.setLoanType("HOME");
            dto.setTotalLoan(100_000);
            dto.setAmountPaid(25_000);
            dto.setOutstandingAmount(75_000);

            assertThat(service.updateLoan(dto)).isTrue();

            assertThat(existing.getLoanType()).isEqualTo("HOME");
            assertThat(existing.getAmountPaid()).isEqualTo(25_000);
            assertThat(existing.getOutstandingAmount()).isEqualTo(75_000);
            verify(loansRepository).save(existing);
        }

        @Test
        void throwsWhenLoanNumberUnknown() {
            when(loansRepository.findByLoanNumber("999")).thenReturn(Optional.empty());
            LoansDto dto = new LoansDto();
            dto.setLoanNumber("999");

            assertThatThrownBy(() -> service.updateLoan(dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Loan not found with loan number:999");
            verify(loansRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteLoan {

        @Test
        void deletesLoanById() {
            when(loansRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(loan(5L, "1234567890123456")));

            assertThat(service.deleteLoan(MOBILE)).isTrue();

            verify(loansRepository).deleteById(5L);
        }

        @Test
        void throwsWhenNoLoanForMobile() {
            when(loansRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteLoan(MOBILE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Loan not found for mobile: " + MOBILE);
            verify(loansRepository, never()).deleteById(any());
        }
    }
}
