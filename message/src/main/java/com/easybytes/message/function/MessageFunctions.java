package com.easybytes.message.function;

import com.easybytes.message.dto.AccountsMsgDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class MessageFunctions {

    private final Logger logger= LoggerFactory.getLogger(MessageFunctions.class);

    @Bean
    public Function<AccountsMsgDto, AccountsMsgDto> email(){
        return account -> {
            logger.info("Sending email to {}",account.email());
            return account;
        };
    }

    @Bean
    public Function<AccountsMsgDto, Long> sms(){
        return accountsMsgDto -> {
            logger.info("Sending sms to {}",accountsMsgDto.mobileNumber());
            return accountsMsgDto.accountNumber();
        };
    }
}
