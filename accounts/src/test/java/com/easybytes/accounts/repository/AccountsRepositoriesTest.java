package com.easybytes.accounts.repository;

import com.easybytes.accounts.audit.AuditAwareImpl;
import com.easybytes.accounts.config.JpaAuditingConfig;
import com.easybytes.accounts.entity.Accounts;
import com.easybytes.accounts.entity.Customer;
import com.easybytes.accounts.support.MySqlTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests against a real MySQL (Testcontainers), with the schema
 * created by {@code src/main/resources/schema.sql}.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({MySqlTestContainerConfig.class, JpaAuditingConfig.class, AuditAwareImpl.class})
class AccountsRepositoriesTest {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private AccountsRepository accountsRepository;

    private Customer saveCustomer(String mobile) {
        Customer c = new Customer();
        c.setName("Jane Doe");
        c.setEmail("jane@example.com");
        c.setMobileNumber(mobile);
        return customerRepository.saveAndFlush(c);
    }

    private Accounts saveAccount(Long customerId, long accountNumber) {
        Accounts a = new Accounts();
        a.setCustomerId(customerId);
        a.setAccountNumber(accountNumber);
        a.setAccountType("Savings");
        a.setBranchAddress("123 Main Street, New York");
        return accountsRepository.saveAndFlush(a);
    }

    @Test
    void customerGetsGeneratedIdAndAuditColumns() {
        Customer saved = saveCustomer("0611111111");

        assertThat(saved.getCustomerId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("ACCOUNTS_SERVICE");
    }

    @Test
    void findsCustomerByMobileNumber() {
        saveCustomer("0622222222");

        assertThat(customerRepository.findByMobileNumber("0622222222"))
                .get().extracting(Customer::getEmail).isEqualTo("jane@example.com");
        assertThat(customerRepository.findByMobileNumber("0699999999")).isEmpty();
    }

    @Test
    void findsAccountByCustomerIdAndChecksExistenceByNumber() {
        Customer customer = saveCustomer("0633333333");
        saveAccount(customer.getCustomerId(), 1_234_567_890L);

        assertThat(accountsRepository.findByCustomerId(customer.getCustomerId()))
                .get().extracting(Accounts::getAccountNumber).isEqualTo(1_234_567_890L);
        assertThat(accountsRepository.existsByAccountNumber(1_234_567_890L)).isTrue();
        assertThat(accountsRepository.existsByAccountNumber(1_111_111_111L)).isFalse();
    }

    @Test
    void deletesAccountsByCustomerId() {
        Customer customer = saveCustomer("0644444444");
        saveAccount(customer.getCustomerId(), 1_500_000_000L);

        accountsRepository.deleteByCustomerId(customer.getCustomerId());
        accountsRepository.flush();

        assertThat(accountsRepository.findByCustomerId(customer.getCustomerId())).isEmpty();
    }

    @Test
    void persistsCommunicationSwitch() {
        Customer customer = saveCustomer("0655555555");
        Accounts account = saveAccount(customer.getCustomerId(), 1_600_000_000L);
        account.setCommunicationSw(true);
        accountsRepository.saveAndFlush(account);

        assertThat(accountsRepository.findById(1_600_000_000L))
                .get().extracting(Accounts::getCommunicationSw).isEqualTo(true);
    }
}
