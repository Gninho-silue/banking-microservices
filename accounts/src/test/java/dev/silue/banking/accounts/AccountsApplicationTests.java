package com.easybytes.accounts;

import com.easybytes.accounts.controller.AccountsController;
import com.easybytes.accounts.controller.CustomerController;
import com.easybytes.accounts.service.IAccountsService;
import com.easybytes.accounts.service.ICustomerService;
import com.easybytes.accounts.support.MySqlTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full application context against a real MySQL (Testcontainers) and the
 * in-memory Spring Cloud Stream test binder instead of Kafka. No Config Server / Eureka needed.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({MySqlTestContainerConfig.class, TestChannelBinderConfiguration.class})
class AccountsApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithAllMainBeans() {
        assertThat(context.getBean(AccountsController.class)).isNotNull();
        assertThat(context.getBean(CustomerController.class)).isNotNull();
        assertThat(context.getBean(IAccountsService.class)).isNotNull();
        assertThat(context.getBean(ICustomerService.class)).isNotNull();
    }
}
