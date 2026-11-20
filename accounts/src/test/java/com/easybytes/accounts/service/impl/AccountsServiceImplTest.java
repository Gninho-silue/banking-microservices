package com.easybytes.accounts.service.impl;

import com.easybytes.accounts.constants.AccountsConstants;
import com.easybytes.accounts.dto.AccountBalanceDto;
import com.easybytes.accounts.dto.AccountsDto;
import com.easybytes.accounts.dto.AccountsMsgDto;
import com.easybytes.accounts.dto.CustomerDto;
import com.easybytes.accounts.entity.Accounts;
import com.easybytes.accounts.entity.Customer;
import com.easybytes.accounts.exception.CustomerAlreadyExistsException;
import com.easybytes.accounts.exception.InvalidTransferException;
import com.easybytes.accounts.exception.ResourceNotFoundException;
import com.easybytes.accounts.repository.AccountsRepository;
import com.easybytes.accounts.repository.CustomerRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountsServiceImplTest {

    private static final String MOBILE = "0612345678";

    @Mock
    private AccountsRepository accountsRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private AccountsServiceImpl service;

    private static CustomerDto customerDto() {
        CustomerDto dto = new CustomerDto();
        dto.setName("Jane Doe");
        dto.setEmail("jane@example.com");
        dto.setMobileNumber(MOBILE);
        return dto;
    }

    private static Customer customer(long id) {
        Customer c = new Customer();
        c.setCustomerId(id);
        c.setName("Jane Doe");
        c.setEmail("jane@example.com");
        c.setMobileNumber(MOBILE);
        return c;
    }

    private static Accounts account(long customerId, long accountNumber) {
        Accounts a = new Accounts();
        a.setCustomerId(customerId);
        a.setAccountNumber(accountNumber);
        a.setAccountType(AccountsConstants.SAVINGS);
        a.setBranchAddress(AccountsConstants.ADDRESS);
        return a;
    }

    @Nested
    class CreateAccount {

        @Test
        void savesCustomerAndDefaultSavingsAccountThenPublishesCommunication() {
            when(customerRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
                Customer c = inv.getArgument(0);
                c.setCustomerId(42L);
                return c;
            });
            when(accountsRepository.existsById(anyLong())).thenReturn(false);
            when(accountsRepository.save(any(Accounts.class))).thenAnswer(inv -> inv.getArgument(0));

            service.createAccount(customerDto());

            ArgumentCaptor<Accounts> accountCaptor = ArgumentCaptor.forClass(Accounts.class);
            verify(accountsRepository).save(accountCaptor.capture());
            Accounts saved = accountCaptor.getValue();
            assertThat(saved.getCustomerId()).isEqualTo(42L);
            assertThat(saved.getAccountType()).isEqualTo(AccountsConstants.SAVINGS);
            assertThat(saved.getBranchAddress()).isEqualTo(AccountsConstants.ADDRESS);
            // 10-digit account number in [1_000_000_000, 1_900_000_000)
            assertThat(saved.getAccountNumber()).isBetween(1_000_000_000L, 1_899_999_999L);

            ArgumentCaptor<Object> msgCaptor = ArgumentCaptor.forClass(Object.class);
            verify(streamBridge).send(eq("sendCommunication-out-0"), msgCaptor.capture());
            assertThat(msgCaptor.getValue()).isEqualTo(new AccountsMsgDto(
                    saved.getAccountNumber(), "Jane Doe", "jane@example.com", MOBILE));
        }

        @Test
        void regeneratesAccountNumberWhileItAlreadyExists() {
            when(customerRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(accountsRepository.existsById(anyLong())).thenReturn(true, true, false);
            when(accountsRepository.save(any(Accounts.class))).thenAnswer(inv -> inv.getArgument(0));

            service.createAccount(customerDto());

            verify(accountsRepository, times(3)).existsById(anyLong());
        }

        @Test
        void rejectsDuplicateMobileNumberWithoutSavingAnything() {
            when(customerRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(customer(1L)));

            assertThatThrownBy(() -> service.createAccount(customerDto()))
                    .isInstanceOf(CustomerAlreadyExistsException.class)
                    .hasMessageContaining(MOBILE);

            verify(customerRepository, never()).save(any());
            verify(accountsRepository, never()).save(any());
            verifyNoInteractions(streamBridge);
        }
    }

    @Nested
    class GetAccountDetails {

        @Test
        void returnsCustomerWithAccount() {
            when(customerRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(customer(7L)));
            when(accountsRepository.findByCustomerId(7L)).thenReturn(Optional.of(account(7L, 1_234_567_890L)));

            CustomerDto result = service.getAccountDetails(MOBILE);

            assertThat(result.getName()).isEqualTo("Jane Doe");
            assertThat(result.getEmail()).isEqualTo("jane@example.com");
            assertThat(result.getMobileNumber()).isEqualTo(MOBILE);
            assertThat(result.getAccountsDto().getAccountNumber()).isEqualTo(1_234_567_890L);
            assertThat(result.getAccountsDto().getAccountType()).isEqualTo(AccountsConstants.SAVINGS);
            assertThat(result.getAccountsDto().getBranchAddress()).isEqualTo(AccountsConstants.ADDRESS);
        }

        @Test
        void throwsWhenCustomerUnknown() {
            when(customerRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAccountDetails(MOBILE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Customer not found with the given input data mobileNumber: " + MOBILE);
        }

        @Test
        void throwsWhenCustomerHasNoAccount() {
            when(customerRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(customer(7L)));
            when(accountsRepository.findByCustomerId(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAccountDetails(MOBILE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account not found with the given input data customerId: 7");
        }
    }

    @Nested
    class UpdateAccount {

        private CustomerDto updateRequest(long accountNumber) {
            AccountsDto accountsDto = new AccountsDto();
            accountsDto.setAccountNumber(accountNumber);
            accountsDto.setAccountType("CURRENT");
            accountsDto.setBranchAddress("1 New Street");
            CustomerDto dto = customerDto();
            dto.setName("Jane Updated");
            dto.setAccountsDto(accountsDto);
            return dto;
        }

        @Test
        void returnsFalseAndTouchesNothingWhenNoAccountGiven() {
            assertThat(service.updateAccount(customerDto())).isFalse();
            verifyNoInteractions(accountsRepository, customerRepository);
        }

        @Test
        void updatesAccountAndCustomer() {
            Accounts existingAccount = account(7L, 1_234_567_890L);
            Customer existingCustomer = customer(7L);
            when(accountsRepository.findById(1_234_567_890L)).thenReturn(Optional.of(existingAccount));
            when(accountsRepository.save(existingAccount)).thenReturn(existingAccount);
            when(customerRepository.findById(7L)).thenReturn(Optional.of(existingCustomer));

            boolean updated = service.updateAccount(updateRequest(1_234_567_890L));

            assertThat(updated).isTrue();
            assertThat(existingAccount.getAccountType()).isEqualTo("CURRENT");
            assertThat(existingAccount.getBranchAddress()).isEqualTo("1 New Street");
            assertThat(existingCustomer.getName()).isEqualTo("Jane Updated");
            verify(customerRepository).save(existingCustomer);
        }

        @Test
        void throwsWhenAccountUnknown() {
            when(accountsRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAccount(updateRequest(99L)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account not found with the given input data AccountNumber: 99");
        }

        @Test
        void throwsWhenAccountOwnerUnknown() {
            Accounts existingAccount = account(7L, 1_234_567_890L);
            when(accountsRepository.findById(1_234_567_890L)).thenReturn(Optional.of(existingAccount));
            when(accountsRepository.save(existingAccount)).thenReturn(existingAccount);
            when(customerRepository.findById(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAccount(updateRequest(1_234_567_890L)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Customer not found with the given input data CustomerID: 7");
            verify(customerRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteAccount {

        @Test
        void deletesAccountThenCustomer() {
            when(customerRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(customer(7L)));

            assertThat(service.deleteAccount(MOBILE)).isTrue();

            verify(accountsRepository).deleteByCustomerId(7L);
            verify(customerRepository).deleteById(7L);
        }

        @Test
        void throwsWhenCustomerUnknown() {
            when(customerRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteAccount(MOBILE))
                    .isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(accountsRepository);
        }
    }

    @Nested
    class UpdateCommunicationStatus {

        @Test
        void returnsFalseForNullAccountNumber() {
            assertThat(service.updateCommunicationStatus(null)).isFalse();
            verifyNoInteractions(accountsRepository);
        }

        @Test
        void flagsCommunicationAsSent() {
            Accounts existing = account(7L, 1_234_567_890L);
            when(accountsRepository.findById(1_234_567_890L)).thenReturn(Optional.of(existing));

            assertThat(service.updateCommunicationStatus(1_234_567_890L)).isTrue();

            assertThat(existing.getCommunicationSw()).isTrue();
            verify(accountsRepository).save(existing);
        }

        @Test
        void throwsWhenAccountUnknown() {
            when(accountsRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCommunicationStatus(5L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class Deposit {

        @Test
        void creditsTheLockedAccountAndReturnsTheNewBalance() {
            Accounts existing = account(7L, 1_234_567_890L);
            existing.setBalance(new BigDecimal("10.00"));
            when(accountsRepository.findByIdForUpdate(1_234_567_890L)).thenReturn(Optional.of(existing));

            AccountBalanceDto result = service.deposit(1_234_567_890L, new BigDecimal("5.5"));

            assertThat(result.getAccountNumber()).isEqualTo(1_234_567_890L);
            assertThat(result.getBalance()).isEqualTo(new BigDecimal("15.50"));
            assertThat(existing.getBalance()).isEqualTo(new BigDecimal("15.50"));
        }

        @Test
        void rejectsNonPositiveAmountWithoutLocking() {
            assertThatThrownBy(() -> service.deposit(1_234_567_890L, new BigDecimal("0")))
                    .isInstanceOf(InvalidTransferException.class);
            verifyNoInteractions(accountsRepository);
        }

        @Test
        void unknownAccountIsNotFound() {
            when(accountsRepository.findByIdForUpdate(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deposit(5L, BigDecimal.ONE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account not found with the given input data accountNumber: 5");
        }
    }
}
