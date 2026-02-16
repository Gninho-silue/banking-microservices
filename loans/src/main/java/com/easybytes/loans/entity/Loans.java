package com.easybytes.loans.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "loans")
public class Loans extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "loan_number", nullable = false, length = 100, unique = true)
    private String loanNumber;

    @Column(name = "loan_type", nullable = false, length = 100)
    private String loanType;

    @Column(name = "total_loan", nullable = false)
    private Integer totalLoan;

    @Column(name = "amount_paid", nullable = false)
    private Integer amountPaid;

    @Column(name = "outstanding_amount", nullable = false)
    private Integer outstandingAmount;
}
