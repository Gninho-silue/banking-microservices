package com.easybytes.loans.service;

import com.easybytes.loans.dto.LoansDto;

public interface ILoansService {

    /**
     * Creates a new loan for the given customer mobile number.
     *
     * @param mobileNumber 10-digit mobile number
     */
    void createLoan(String mobileNumber);

    /**
     * Retrieves the loan details associated with the given mobile number.
     *
     * @param mobileNumber 10-digit mobile number
     * @return the loan details
     */
    LoansDto fetchLoan(String mobileNumber);

    /**
     * Updates the loan details for an existing customer mobile.
     *
     * @param loansDto updated details
     * @return true if updated
     */
    boolean updateLoan(LoansDto loansDto);

    /**
     * Deletes the loan associated with the given mobile number.
     *
     * @param mobileNumber 10-digit mobile number
     * @return true if deleted
     */
    boolean deleteLoan(String mobileNumber);
}
