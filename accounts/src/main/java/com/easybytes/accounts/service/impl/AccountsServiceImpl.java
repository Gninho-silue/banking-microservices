package com.easybytes.accounts.service.impl;

import com.easybytes.accounts.constants.AccountsConstants;
import com.easybytes.accounts.dto.AccountsMsgDto;
import com.easybytes.accounts.mapper.CustomerMapper;
import com.easybytes.accounts.dto.AccountsDto;
import com.easybytes.accounts.dto.CustomerDto;
import com.easybytes.accounts.entity.Accounts;
import com.easybytes.accounts.entity.Customer;
import com.easybytes.accounts.exception.CustomerAlreadyExistsException;
import com.easybytes.accounts.exception.ResourceNotFoundException;
import com.easybytes.accounts.mapper.AccountsMapper;
import com.easybytes.accounts.repository.AccountsRepository;
import com.easybytes.accounts.repository.CustomerRepository;
import com.easybytes.accounts.service.IAccountsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AccountsServiceImpl implements IAccountsService {

    private final AccountsRepository accountsRepository;
    private final CustomerRepository customerRepository;
    private final StreamBridge streamBridge;
    private final Logger logger = LoggerFactory.getLogger(AccountsServiceImpl.class);

    /**
     * Creates a new account for the given customer.
     *
     * @param customerDto the customer details required to create the account,
     *                    including the customer's name, email, and phone number
     */
    @Override
    public void createAccount(CustomerDto customerDto) {
        Customer customer = CustomerMapper.toCustomer(customerDto, new Customer());
        Optional<Customer> optionalCustomer = customerRepository.findByMobileNumber(customer.getMobileNumber());
        if (optionalCustomer.isPresent()) {
            throw new CustomerAlreadyExistsException("Customer already registered with given mobile number:"
                    + customer.getMobileNumber());
        }

        Customer savedCostumer = customerRepository.save(customer);
        Accounts savedAccount = accountsRepository.save(createNewAccount(savedCostumer));
        sendCommunication(savedAccount,  savedCostumer);
    }

    /**
     * Retrieves the account details associated with the given phone number.
     *
     * @param phone the phone number of the customer whose account details are to be
     *              retrieved;
     *              must be a valid 10-digit phone number
     * @return the customer details including name, email, and phone as a
     *         {@code CustomerDto} object
     */
    @Override
    public CustomerDto getAccountDetails(String phone) {
        Customer customer = customerRepository.findByMobileNumber(phone).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", phone));

        Accounts account = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString()));

        CustomerDto customerDto = CustomerMapper.toCustomerDto(customer, new CustomerDto());
        customerDto.setAccountsDto(AccountsMapper.toAccountsDto(account, new AccountsDto()));
        return customerDto;
    }

    /**
     * Updates the account details for an existing customer.
     *
     * @param customerDto the customer details to be updated, including their name,
     *                    email, phone number,
     *                    and any associated account details; must not be null and
     *                    must contain valid values
     * @return {@code true} if the account details were successfully updated,
     *         {@code false} otherwise
     */
    @Override
    public boolean updateAccount(CustomerDto customerDto) {
        boolean isUpdated = false;
        AccountsDto accountsDto = customerDto.getAccountsDto();
        if (accountsDto != null) {
            Accounts accounts = accountsRepository.findById(accountsDto.getAccountNumber()).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "AccountNumber",
                            accountsDto.getAccountNumber().toString()));
            AccountsMapper.toAccounts(accountsDto, accounts);
            accounts = accountsRepository.save(accounts);

            Long customerId = accounts.getCustomerId();
            Customer customer = customerRepository.findById(customerId).orElseThrow(
                    () -> new ResourceNotFoundException("Customer", "CustomerID", customerId.toString())

            );
            CustomerMapper.toCustomer(customerDto, customer);
            customerRepository.save(customer);
            isUpdated = true;
        }
        return isUpdated;
    }

    /**
     * Deletes the account associated with the given phone number.
     *
     * @param phone the phone number of the customer whose account is to be deleted;
     *              must be a valid 10-digit phone number
     * @return {@code true} if the account was successfully deleted, {@code false}
     *         otherwise
     */
    @Override
    public boolean deleteAccount(String phone) {
        Customer customer = customerRepository.findByMobileNumber(phone).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", phone));

        accountsRepository.deleteByCustomerId(customer.getCustomerId());
        customerRepository.deleteById(customer.getCustomerId());
        return true;
    }

    /**
     * Updates the communication status of a specific account.
     *
     * @param accountNumber the unique identifier of the account for which the communication
     *                      status needs to be updated; must not be null and must comply
     *                      with the account number format
     * @return true if the communication status was successfully updated, false otherwise
     */
    @Override
    public boolean updateCommunicationStatus(Long accountNumber) {
        boolean isUpdated = false;
        if (accountNumber != null){
            Accounts account = accountsRepository.findById(accountNumber).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "AccountNumber", accountNumber.toString())
            );
            account.setCommunicationSw(true);
            accountsRepository.save(account);
            isUpdated = true;
            return isUpdated;
        }
        return isUpdated;
    }

    /**
     * Creates a new account associated with the given customer.
     *
     * @param customer the customer entity for whom the account is being created
     * @return the newly created account instance associated with the customer
     */
    private Accounts createNewAccount(Customer customer) {
        Accounts newAccount = new Accounts();
        newAccount.setCustomerId(customer.getCustomerId());
        newAccount.setAccountNumber(generateUniqueAccountNumber());
        newAccount.setAccountType(AccountsConstants.SAVINGS);
        newAccount.setBranchAddress(AccountsConstants.ADDRESS);
        return newAccount;
    }

    /**
     * Generates a unique 9-digit account number.
     * Checks against the repository to ensure no duplicates exist.
     *
     * @return a unique account number
     */
    private long generateUniqueAccountNumber() {
        long accountNumber;
        do {
            accountNumber = 1000000000L + new Random().nextInt(900000000);
        } while (accountsRepository.existsById(accountNumber));
        return accountNumber;
    }

    private void sendCommunication(Accounts account, Customer customer) {
        var accountsMsgDto = new AccountsMsgDto(account.getAccountNumber(), customer.getName(),
                customer.getEmail(), customer.getMobileNumber());
        logger.info("Sending Communication request for the details: {}", accountsMsgDto);
        var result = streamBridge.send("sendCommunication-out-0", accountsMsgDto);
        logger.info("Is the Communication request successfully triggered ? : {}", result);
    }

}
