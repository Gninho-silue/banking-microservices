package dev.silue.banking.loans.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * System clock as a bean, so time-dependent code (loan start date) can be tested with a fixed clock.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
