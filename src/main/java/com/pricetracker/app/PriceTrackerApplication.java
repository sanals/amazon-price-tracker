package com.pricetracker.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Main application class for the Price Tracker application.
 * This class serves as the entry point for the Spring Boot application.
 */
@SpringBootApplication
@EnableScheduling    // Enable scheduling for price check tasks
@EnableJpaAuditing   // Enable JPA auditing for entity timestamps
@EnableAsync         // Enable asynchronous method execution
public class PriceTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriceTrackerApplication.class, args);
    }
    
    /**
     * Configure the async task executor.
     * This executor is used for asynchronous operations like sending emails.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("PriceTracker-");
        executor.initialize();
        return executor;
    }
} 