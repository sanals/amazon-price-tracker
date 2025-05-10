package com.pricetracker.app.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Properties class for binding CORS configuration from application.yml
 * 
 * This class maps properties under the "cors" prefix in application.yml
 * to Java objects for type-safe access.
 */
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    private List<String> allowedOrigins;
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
    private Long maxAge;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public Long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Long maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public String toString() {
        return "CorsProperties{" +
                "allowedOrigins=" + allowedOrigins +
                ", allowedMethods=" + allowedMethods +
                ", allowedHeaders=" + allowedHeaders +
                ", maxAge=" + maxAge +
                '}';
    }
}