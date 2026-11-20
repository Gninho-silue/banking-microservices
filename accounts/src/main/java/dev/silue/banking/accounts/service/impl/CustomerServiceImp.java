package dev.silue.banking.accounts.service.impl;

import dev.silue.banking.accounts.dto.AccountsDto;
import dev.silue.banking.accounts.dto.CardsDto;
import dev.silue.banking.accounts.dto.CustomerDetailsDto;
import dev.silue.banking.accounts.dto.LoansDto;
import dev.silue.banking.accounts.entity.Accounts;
import dev.silue.banking.accounts.entity.Customer;
import dev.silue.banking.accounts.exception.ResourceNotFoundException;
import dev.silue.banking.accounts.mapper.AccountsMapper;
import dev.silue.banking.accounts.mapper.CustomerMapper;
import dev.silue.banking.accounts.repository.AccountsRepository;
import dev.silue.banking.accounts.repository.CustomerRepository;
import dev.silue.banking.accounts.service.ICustomerService;
import dev.silue.banking.accounts.service.client.CardsFeignClient;
import dev.silue.banking.accounts.service.client.LoansFeignClient;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerServiceImp implements ICustomerService {

    private final AccountsRepository accountsRepository;
    private final CustomerRepository customerRepository;
    private final CardsFeignClient cardsFeignClient;
    private final LoansFeignClient loansFeignClient;

    /**
     * @param mobileNumber  -Input Mobile Number
     * @param correlationId - Tracking id
     * @return Customer Details based on a given mobileNumber
     */
    @Override
    public CustomerDetailsDto fetchCustomerDetails(String mobileNumber, String correlationId) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));

        Accounts account = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
        );

        CustomerDetailsDto customerDetailsDto = CustomerMapper.toCustomerDetailsDto(customer, new CustomerDetailsDto());
        customerDetailsDto.setAccountsDto(AccountsMapper.toAccountsDto(account, new AccountsDto()));

        ResponseEntity<LoansDto> loansDtoResponseEntity = loansFeignClient.fetchLoanDetails(correlationId, mobileNumber);
        if (loansDtoResponseEntity != null) {
            customerDetailsDto.setLoansDto(loansDtoResponseEntity.getBody());
        }
        ResponseEntity<CardsDto> cardsDtoResponseEntity = cardsFeignClient.fetchCardDetails(correlationId, mobileNumber);
        if (cardsDtoResponseEntity != null) {
            customerDetailsDto.setCardsDto(cardsDtoResponseEntity.getBody());
        }
        return customerDetailsDto;
    }
}
