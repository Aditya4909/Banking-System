package com.javabank;

import com.javabank.analytics.AnalyticsService;
import com.javabank.analytics.AnalyticsServiceImpl;
import com.javabank.model.User;
import com.javabank.repository.*;
import com.javabank.service.BankService;
import com.javabank.service.BankServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

/**
 * Spring Boot Web Application entry point for JavaBank.
 * Exposes the core service layer over REST APIs and hosts the glassmorphic web dashboard.
 */
@SpringBootApplication
public class JavaBankApplication {

    @Bean
    public UserRepository userRepository() {
        return new UserRepositoryImpl();
    }

    @Bean
    public AccountRepository accountRepository() {
        return new InMemoryAccountRepository();
    }

    @Bean
    public TransactionRepository transactionRepository() {
        return new InMemoryTransactionRepository();
    }

    @Bean
    public PersistenceService persistenceService() throws IOException {
        return new FilePersistenceService("data");
    }

    @Bean
    public BankService bankService(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            PersistenceService persistenceService
    ) throws IOException {
        BankService service = new BankServiceImpl(userRepository, accountRepository, transactionRepository, persistenceService);
        
        // Seed initial demo customer and accounts
        try {
            User demoUser = new User("CUST-1001", "John Doe", "john.doe@example.com");
            service.createUser(demoUser);
            service.createAccount("CUST-1001", "Savings", 5000.0, 0.025); // 2.5% APY
            service.createAccount("CUST-1001", "Current", 1500.0, 800.0);  // $800 overdraft
        } catch (Exception e) {
            // Ignore if already initialized
        }
        
        return service;
    }

    @Bean
    public AnalyticsService analyticsService() {
        return new AnalyticsServiceImpl();
    }

    public static void main(String[] args) {
        SpringApplication.run(JavaBankApplication.class, args);
    }
}
