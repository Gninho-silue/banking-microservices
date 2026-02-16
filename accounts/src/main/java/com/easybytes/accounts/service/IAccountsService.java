package com.easybytes.accounts.service;

import com.easybytes.accounts.dto.CustomerDto;

public interface IAccountsService {

    /**
     * Creates a new account for the given customer.
     *
     * @param customerDto the customer details required to create the account,
     *                    including the customer's name, email, and mobile number
     */
    void createAccount(CustomerDto customerDto);

    /**
     * Retrieves the account details associated with the given phone number.
     *
     * @param phone the phone number of the customer whose account details are to be retrieved;
     *              must be a valid 10-digit phone number
     * @return the customer details including name, email, and phone as a {@code CustomerDto} object
     */
    CustomerDto getAccountDetails(String phone);

    /**
     * Updates the account details for an existing customer.
     *
     * @param customerDto the customer details to be updated, including their name, email, phone number,
     *                    and any associated account details; must not be null and must contain valid values
     * @return {@code true} if the account details were successfully updated, {@code false} otherwise
     */
    boolean updateAccount(CustomerDto customerDto);

    /**
     * Deletes the account associated with the given phone number.
     *
     * @param phone the phone number of the customer whose account is to be deleted;
     *              must be a valid 10-digit phone number
     * @return {@code true} if the account was successfully deleted, {@code false} otherwise
     */
    boolean deleteAccount(String phone);

    /**
 * Updates the communication status of a specific account.
 *
 * @param accountNumber the unique identifier of the account for which the communication
 *                      status needs to be updated; must not be null and must comply
 *                      with the account number format
 * @return true if the communication status was successfully updated, false otherwise
 */
    boolean updateCommunicationStatus(Long accountNumber);
}
