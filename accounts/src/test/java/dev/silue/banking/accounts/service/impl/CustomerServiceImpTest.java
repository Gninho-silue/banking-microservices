package com.easybytes.accounts.service.impl;

import com.easybytes.accounts.dto.CardsDto;
import com.easybytes.accounts.dto.CustomerDetailsDto;
import com.easybytes.accounts.dto.LoansDto;
import com.easybytes.accounts.entity.Accounts;
import com.easybytes.accounts.entity.Customer;
import com.easybytes.accounts.exception.ResourceNotFoundException;
import com.easybytes.accounts.repository.AccountsRepository;
import com.easybytes.accounts.repository.CustomerRepository;
import com.easybytes.accounts.service.client.CardsFallback;
import com.easybytes.accounts.service.client.CardsFeignClient;
import com.easybytes.accounts.service.client.LoansFallback;
import com.easybytes.accounts.service.client.LoansFeignClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImpTest {

    private static final String MOBILE = "0612345678";
    private static final String CORRELATION_ID = "corr-123";

    @Mock
    private AccountsRepository accountsRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CardsFeignClient cardsFeignClient;
    @Mock
    private LoansFeignClient loansFeignClient;

    private CustomerServiceImp serviceWith(CardsFeignClient cards, LoansFeignClient loans) {
        return new CustomerServiceImp(accountsRepository, customerRepository, cards, loans);
    }

    private void givenExistingCustomerWithAccount() {
        Customer customer = new Customer(7L, "Jane Doe", "jane@example.com", MOBILE);
        Accounts account = new Accounts(7L, 1_234_567_890L, "Savings", "123 Main Street, New York", null, new BigDecimal("0.00"));
        when(customerRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(customer));
        when(accountsRepository.findByCustomerId(7L)).thenReturn(Optional.of(account));
    }

    @Test
    void aggregatesCustomerAccountLoanAndCardAndPropagatesCorrelationId() {
        givenExistingCustomerWithAccount();
        LoansDto loan = new LoansDto();
        loan.setLoanNumber("100000000001");
        CardsDto card = new CardsDto();
        card.setCardNumber("400000000001");
        when(loansFeignClient.fetchLoanDetails(CORRELATION_ID, MOBILE)).thenReturn(ResponseEntity.ok(loan));
        when(cardsFeignClient.fetchCardDetails(CORRELATION_ID, MOBILE)).thenReturn(ResponseEntity.ok(card));

        CustomerDetailsDto details = serviceWith(cardsFeignClient, loansFeignClient)
                .fetchCustomerDetails(MOBILE, CORRELATION_ID);

        assertThat(details.getName()).isEqualTo("Jane Doe");
        assertThat(details.getEmail()).isEqualTo("jane@example.com");
        assertThat(details.getMobileNumber()).isEqualTo(MOBILE);
        assertThat(details.getAccountsDto().getAccountNumber()).isEqualTo(1_234_567_890L);
        assertThat(details.getLoansDto()).isSameAs(loan);
        assertThat(details.getCardsDto()).isSameAs(card);
    }

    @Test
    void toleratesNullResponsesFromDownstreamClients() {
        givenExistingCustomerWithAccount();
        when(loansFeignClient.fetchLoanDetails(CORRELATION_ID, MOBILE)).thenReturn(null);
        when(cardsFeignClient.fetchCardDetails(CORRELATION_ID, MOBILE)).thenReturn(null);

        CustomerDetailsDto details = serviceWith(cardsFeignClient, loansFeignClient)
                .fetchCustomerDetails(MOBILE, CORRELATION_ID);

        assertThat(details.getAccountsDto()).isNotNull();
        assertThat(details.getLoansDto()).isNull();
        assertThat(details.getCardsDto()).isNull();
    }

    @Test
    void degradesGracefullyWhenCircuitBreakerFallbacksAreUsed() {
        givenExistingCustomerWithAccount();

        CustomerDetailsDto details = serviceWith(new CardsFallback(), new LoansFallback())
                .fetchCustomerDetails(MOBILE, CORRELATION_ID);

        assertThat(details.getName()).isEqualTo("Jane Doe");
        assertThat(details.getAccountsDto().getAccountNumber()).isEqualTo(1_234_567_890L);
        assertThat(details.getLoansDto()).isNull();
        assertThat(details.getCardsDto()).isNull();
    }

    @Test
    void throwsWhenCustomerUnknownWithoutCallingDownstreamServices() {
        when(customerRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceWith(cardsFeignClient, loansFeignClient)
                .fetchCustomerDetails(MOBILE, CORRELATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found with the given input data mobileNumber: " + MOBILE);
        verifyNoInteractions(cardsFeignClient, loansFeignClient);
    }

    @Test
    void throwsWhenCustomerHasNoAccount() {
        when(customerRepository.findByMobileNumber(MOBILE))
                .thenReturn(Optional.of(new Customer(7L, "Jane Doe", "jane@example.com", MOBILE)));
        when(accountsRepository.findByCustomerId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceWith(cardsFeignClient, loansFeignClient)
                .fetchCustomerDetails(MOBILE, CORRELATION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(cardsFeignClient, loansFeignClient);
    }
}
