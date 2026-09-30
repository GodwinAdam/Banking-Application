package com.bankingapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// @Configuration marks this class as a source of @Bean definitions - a
// place where we manually construct and register objects into the Spring
// container, rather than relying on @Component/@Service auto-detection.
public class OpenApiConfig {

    @Bean
    // Every method annotated @Bean inside a @Configuration class runs once
    // at startup, and its return value is registered in the Spring
    // container so any other bean (or springdoc itself) can use it.
    // Here we're just customizing the title/description shown at the top
    // of the Swagger UI page - entirely optional, purely cosmetic.
    public OpenAPI bankingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking Application API")
                        .description("Learning project: customers, accounts, and transactions REST API")
                        .version("1.0.0")
                        .contact(new Contact().name("Banking App").email("dev@example.com")));
    }
}
