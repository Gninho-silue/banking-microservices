package com.easybytes.loans;

import com.easybytes.loans.controller.LoansController;
import com.easybytes.loans.service.ILoansService;
import com.easybytes.loans.support.MySqlTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full application context against a real MySQL (Testcontainers).
 * No Config Server / Eureka needed (see application-test.yml).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(MySqlTestContainerConfig.class)
class LoansApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithAllMainBeans() {
        assertThat(context.getBean(LoansController.class)).isNotNull();
        assertThat(context.getBean(ILoansService.class)).isNotNull();
    }
}
