package com.easybytes.accounts.entity;

import com.easybytes.accounts.exception.InsufficientFundsException;
import com.easybytes.accounts.service.rules.TransferRules;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Accounts extends BaseEntity {

    @Column(name = "customer_id")
    private Long customerId;

    @Id
    @Column(name = "account_number")
    private Long  accountNumber;

    private String accountType;

    @Column(name = "branch_address")
    private String branchAddress;
    @Column(name = "communication_sw")
    private Boolean communicationSw;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = TransferRules.toMoney(BigDecimal.ZERO);

    /**
     * Adds money to the account. The amount is expected to be already validated (> 0).
     */
    public void credit(BigDecimal amount) {
        balance = TransferRules.toMoney(balance.add(amount));
    }

    /**
     * Removes money from the account; the balance can never become negative.
     *
     * @throws InsufficientFundsException if the balance is lower than the amount
     */
    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountNumber);
        }
        balance = TransferRules.toMoney(balance.subtract(amount));
    }
}
