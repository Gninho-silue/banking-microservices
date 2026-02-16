package com.easybytes.cards;

import com.easybytes.cards.dto.ContactInfoDto;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableConfigurationProperties(value = {ContactInfoDto.class})
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
@OpenAPIDefinition(info = @Info(title = "Cards microservice REST API Documentation",
        description = "EasyBank Cards REST API Documentation",
        version = "v1",
        contact = @Contact(
                name = "Dev225Tech",
                email = "dev225tech@gamil.com",
                url = "https://dev225tech.com"
        ),
        license = @License(
                name = "Apache 2.0",
                url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
),
        externalDocs = @ExternalDocumentation(description = "EasyBank Cards REST API Documentation",
                url = "https://easybank.dev225tech.com/cards/v1/api-docs")
)
public class CardsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardsApplication.class, args);
    }

}
