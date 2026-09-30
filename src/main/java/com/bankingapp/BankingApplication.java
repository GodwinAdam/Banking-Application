package com.bankingapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the whole program. When you run this class's main() method,
 * Spring Boot starts an embedded Tomcat server, wires up every @Component /
 * @Service / @Repository / @RestController it can find under this package,
 * and connects to the database defined in application.yml.
 */
@SpringBootApplication
// @SpringBootApplication is itself a combination of three annotations:
//   1. @Configuration      - this class can declare Spring @Bean methods
//   2. @EnableAutoConfiguration - Spring Boot guesses sensible defaults
//                             (e.g. "H2 driver is on the classpath and no
//                             DataSource bean exists -> auto-create one")
//   3. @ComponentScan      - Spring scans com.bankingapp and all sub-packages
//                             for classes annotated with @Component,
//                             @Service, @Repository, @RestController, etc.,
//                             and registers them as "beans" it manages.
public class BankingApplication {

    public static void main(String[] args) {
        // SpringApplication.run() bootstraps the Spring container (the
        // "ApplicationContext"), starts the embedded web server, and blocks
        // to keep the JVM alive while it serves HTTP requests.
        SpringApplication.run(BankingApplication.class, args);
    }
}
