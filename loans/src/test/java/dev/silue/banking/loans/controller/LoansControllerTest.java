package com.easybytes.loans.controller;

import com.easybytes.loans.dto.LoanScheduleDto;
import com.easybytes.loans.dto.LoansDto;
import com.easybytes.loans.exception.InvalidLoanTermsException;
import com.easybytes.loans.exception.LoanAlreadyExistsException;
import com.easybytes.loans.exception.ResourceNotFoundException;
import com.easybytes.loans.service.ILoansService;
import com.easybytes.loans.service.schedule.Installment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoansController.class)
@ActiveProfiles("test")
class LoansControllerTest {

    private static final String MOBILE = "0612345678";
    private static final String CORRELATION_HEADER = "easybank-correlation-id";
    private static final String VALID_LOAN = """
            {"mobileNumber":"0612345678","loanNumber":"1234567890123456","loanType":"HOME",
             "totalLoan":100000,"amountPaid":1000,"outstandingAmount":99000}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ILoansService loansService;

    @Test
    void createReturns201() throws Exception {
        mockMvc.perform(post("/api/create").param("mobileNumber", MOBILE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("201"))
                .andExpect(jsonPath("$.statusMessage").value("Loan created successfully"));

        verify(loansService).createLoan(MOBILE);
    }

    @Test
    void createForExistingLoanReturns400() throws Exception {
        doThrow(new LoanAlreadyExistsException("Loan already exists for mobile: " + MOBILE))
                .when(loansService).createLoan(MOBILE);

        mockMvc.perform(post("/api/create").param("mobileNumber", MOBILE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.apiPath").value("uri=/api/create"))
                .andExpect(jsonPath("$.errorCode").value("400 BAD_REQUEST"))
                .andExpect(jsonPath("$.errorMessage").value("Loan already exists for mobile: " + MOBILE))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createWithMalformedMobileNumberReturns400() throws Exception {
        mockMvc.perform(post("/api/create").param("mobileNumber", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.apiPath").value("uri=/api/create"))
                .andExpect(jsonPath("$.errorCode").value("400 BAD_REQUEST"))
                .andExpect(jsonPath("$.errorMessage").value("Mobile number must be 10 digits"));

        verifyNoInteractions(loansService);
    }

    @Test
    void fetchWithMalformedMobileNumberReturns400() throws Exception {
        mockMvc.perform(get("/api/fetch").header(CORRELATION_HEADER, "corr-1").param("mobileNumber", "12ab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value(containsString("10 digits")));

        verifyNoInteractions(loansService);
    }

    @Test
    void fetchReturnsLoan() throws Exception {
        LoansDto dto = new LoansDto();
        dto.setMobileNumber(MOBILE);
        dto.setLoanNumber("1234567890123456");
        dto.setTotalLoan(new BigDecimal("100000.00"));
        when(loansService.fetchLoan(MOBILE)).thenReturn(dto);

        mockMvc.perform(get("/api/fetch").header(CORRELATION_HEADER, "corr-1").param("mobileNumber", MOBILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanNumber").value("1234567890123456"))
                .andExpect(jsonPath("$.totalLoan").value(100000.00));
    }

    @Test
    void fetchWithoutCorrelationHeaderReturns400() throws Exception {
        mockMvc.perform(get("/api/fetch").param("mobileNumber", MOBILE))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(loansService);
    }

    @Test
    void fetchUnknownLoanReturns404() throws Exception {
        when(loansService.fetchLoan(MOBILE))
                .thenThrow(new ResourceNotFoundException("Loans not found for mobile: " + MOBILE));

        mockMvc.perform(get("/api/fetch").header(CORRELATION_HEADER, "corr-1").param("mobileNumber", MOBILE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("404 NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("Loans not found for mobile: " + MOBILE));
    }

    @Test
    void updateReturns200WhenUpdated() throws Exception {
        when(loansService.updateLoan(any())).thenReturn(true);

        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(VALID_LOAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"));
    }

    @Test
    void updateReturns417WhenNotUpdated() throws Exception {
        when(loansService.updateLoan(any())).thenReturn(false);

        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(VALID_LOAN))
                .andExpect(status().isExpectationFailed())
                .andExpect(jsonPath("$.statusCode").value("417"));
    }

    @Test
    void updateWithInvalidBodyReturns400WithFieldErrors() throws Exception {
        String invalid = """
                {"mobileNumber":"12","loanNumber":"1234567890123456","loanType":"BOAT",
                 "totalLoan":-1,"amountPaid":0,"outstandingAmount":0}
                """;

        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mobileNumber").value("Mobile number must 10 digits"))
                .andExpect(jsonPath("$.loanType").value("Loan Type must be HOME, PERSONAL or AUTO"))
                .andExpect(jsonPath("$.totalLoan").value("Total loan must be >= 0"));

        verifyNoInteractions(loansService);
    }

    @Test
    void updateUnknownLoanReturns404() throws Exception {
        when(loansService.updateLoan(any())).thenThrow(new ResourceNotFoundException("Loan not found"));

        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(VALID_LOAN))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns200WhenDeleted() throws Exception {
        when(loansService.deleteLoan(MOBILE)).thenReturn(true);

        mockMvc.perform(delete("/api/delete").param("mobileNumber", MOBILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusMessage").value("Request processed successfully"));
    }

    @Test
    void deleteReturns417WhenNotDeleted() throws Exception {
        when(loansService.deleteLoan(MOBILE)).thenReturn(false);

        mockMvc.perform(delete("/api/delete").param("mobileNumber", MOBILE))
                .andExpect(status().isExpectationFailed())
                .andExpect(jsonPath("$.statusCode").value("417"));
    }

    @Test
    void unexpectedErrorReturns500WithErrorResponse() throws Exception {
        when(loansService.fetchLoan(MOBILE)).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/fetch").header(CORRELATION_HEADER, "corr-1").param("mobileNumber", MOBILE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("500 INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.errorMessage").value("boom"));
    }

    @Test
    void scheduleReturnsInstallmentsAndTotals() throws Exception {
        LoanScheduleDto dto = new LoanScheduleDto();
        dto.setLoanNumber("1234567890123456");
        dto.setPrincipal(new BigDecimal("100000.00"));
        dto.setAnnualInterestRate(new BigDecimal("0.0500"));
        dto.setTermMonths(12);
        dto.setMonthlyPayment(new BigDecimal("8560.75"));
        dto.setTotalInterest(new BigDecimal("2729.01"));
        dto.setTotalPayment(new BigDecimal("102729.01"));
        dto.setInstallments(List.of(new Installment(1, LocalDate.of(2026, 2, 15), new BigDecimal("8560.75"),
                new BigDecimal("416.67"), new BigDecimal("8144.08"), new BigDecimal("91855.92"))));
        when(loansService.getSchedule(MOBILE)).thenReturn(dto);

        mockMvc.perform(get("/api/loans/schedule").param("mobileNumber", MOBILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPayment").value(8560.75))
                .andExpect(jsonPath("$.totalPayment").value(102729.01))
                .andExpect(jsonPath("$.installments[0].n").value(1))
                .andExpect(jsonPath("$.installments[0].dueDate").value("2026-02-15"))
                .andExpect(jsonPath("$.installments[0].interest").value(416.67))
                .andExpect(jsonPath("$.installments[0].principal").value(8144.08))
                .andExpect(jsonPath("$.installments[0].remaining").value(91855.92));
    }

    @Test
    void scheduleOfUnknownLoanIs404() throws Exception {
        when(loansService.getSchedule(MOBILE))
                .thenThrow(new ResourceNotFoundException("Loans not found for mobile: " + MOBILE));

        mockMvc.perform(get("/api/loans/schedule").param("mobileNumber", MOBILE))
                .andExpect(status().isNotFound());
    }

    @Test
    void scheduleWithMalformedMobileIs400() throws Exception {
        mockMvc.perform(get("/api/loans/schedule").param("mobileNumber", "12"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(loansService);
    }

    @Test
    void scheduleWithInvalidStoredTermsIs400() throws Exception {
        when(loansService.getSchedule(MOBILE)).thenThrow(new InvalidLoanTermsException("Term must be at least 1 month"));

        mockMvc.perform(get("/api/loans/schedule").param("mobileNumber", MOBILE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Term must be at least 1 month"));
    }

    @Test
    void updateWithNegativeRateOrZeroTermIs400() throws Exception {
        String invalid = """
                {"mobileNumber":"0612345678","loanNumber":"1234567890123456","loanType":"HOME",
                 "totalLoan":100000,"amountPaid":0,"outstandingAmount":100000,
                 "annualInterestRate":-0.01,"termMonths":0}
                """;

        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.annualInterestRate").value("Annual interest rate must be >= 0"))
                .andExpect(jsonPath("$.termMonths").value("Term must be at least 1 month"));

        verifyNoInteractions(loansService);
    }

    @Test
    void contactInfoReturns200() throws Exception {
        mockMvc.perform(get("/api/contact"))
                .andExpect(status().isOk());
    }
}
