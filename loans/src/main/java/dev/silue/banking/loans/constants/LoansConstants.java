package dev.silue.banking.loans.constants;

import java.math.BigDecimal;

public final class LoansConstants {


    private LoansConstants() {
        // restrict instantiation
    }

    public static final String HOME_LOAN = "HOME";
    public static final String PERSONAL_LOAN = "PERSONAL";
    public static final String AUTO_LOAN = "AUTO";
    public static final BigDecimal NEW_LOAN_LIMIT = new BigDecimal("100000.00");
    /** Default terms of a new loan: 5 % nominal annual rate over 60 months, starting on the creation date. */
    public static final BigDecimal DEFAULT_ANNUAL_INTEREST_RATE = new BigDecimal("0.0500");
    public static final int DEFAULT_TERM_MONTHS = 60;

    public static final String STATUS_201 = "201";
    public static final String MESSAGE_201 = "Loan created successfully";
    public static final String STATUS_200 = "200";
    public static final String MESSAGE_200 = "Request processed successfully";
    public static final String STATUS_417 = "417";
    public static final String MESSAGE_417_UPDATE = "Update operation failed. Please try again or contact Dev team";
    public static final String MESSAGE_417_DELETE = "Delete operation failed. Please try again or contact Dev team";
}
