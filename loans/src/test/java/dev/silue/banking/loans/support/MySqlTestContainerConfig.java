package dev.silue.banking.loans.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Provides a throw-away MySQL instance (same major version as docker-compose) for tests.
 * Spring Boot wires the datasource to it via {@link ServiceConnection}; {@code schema.sql}
 * is then applied by {@code spring.sql.init.mode=always}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestContainerConfig {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer("mysql:8.4").withDatabaseName("loansdb");
    }
}
