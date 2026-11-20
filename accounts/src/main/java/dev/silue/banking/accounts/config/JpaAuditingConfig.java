package com.easybytes.accounts.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA auditing lives in its own configuration (instead of on the main class) so that
 * sliced tests such as {@code @WebMvcTest} do not require a JPA metamodel.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
public class JpaAuditingConfig {
}
