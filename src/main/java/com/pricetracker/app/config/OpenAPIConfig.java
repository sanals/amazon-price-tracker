package com.pricetracker.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI priceTrackerOpenAPI() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort + contextPath)
                .description("Local Development Server");

        Contact contact = new Contact()
                .name("Price Tracker Support")
                .email("support@pricetracker.example.com")
                .url("https://pricetracker.example.com/support");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Price Tracker API")
                .description("REST API for tracking product prices across various online stores")
                .version("1.0.0")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
} 