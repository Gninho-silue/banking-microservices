package com.easybytes.accounts.functions;

import com.easybytes.accounts.service.IAccountsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class AccountsFunctions {

    private final Logger logger = LoggerFactory.getLogger(AccountsFunctions.class);

    @Bean
    public Consumer<Long> updateCommunication(IAccountsService accountsService) {
        return accountNumber -> {
            logger.info("Updating communication for account number: " + accountNumber.toString());
            accountsService.updateCommunicationStatus(accountNumber);
        };

    }
}
