package com.pricetracker.app.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.pricetracker.app.util.AppConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

/**
 * Jackson Configuration
 * 
 * Configures the Jackson ObjectMapper to support Java 8 date/time types
 * like LocalDateTime, LocalDate, etc.
 */
@Configuration
public class JacksonConfig {

        private static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter
                        .ofPattern(AppConstants.DEFAULT_DATETIME_FORMAT);

        private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter
                        .ofPattern(AppConstants.DEFAULT_DATE_FORMAT_ISO);

        private static final DateTimeFormatter ZONED_DATETIME_FORMATTER = DateTimeFormatter
                        .ofPattern(AppConstants.DEFAULT_DATETIME_FORMAT);

        /**
         * Creates a new ObjectMapper with JavaTimeModule to support Java 8 date/time
         * types and configures it to serialize LocalDateTime as ISO strings
         * 
         * @return Configured ObjectMapper
         */
        @Bean
        @Primary
        public ObjectMapper objectMapper() {
                ObjectMapper objectMapper = new ObjectMapper();

                // Register JavaTimeModule to handle all Java 8 date/time types
                JavaTimeModule javaTimeModule = new JavaTimeModule();

                // Add custom serializers for date/time types
                javaTimeModule.addSerializer(
                                LocalDateTime.class,
                                new LocalDateTimeSerializer(ISO_DATETIME_FORMATTER));

                javaTimeModule.addSerializer(
                                LocalDate.class,
                                new LocalDateSerializer(ISO_DATE_FORMATTER));

                javaTimeModule.addSerializer(
                                ZonedDateTime.class,
                                new ZonedDateTimeSerializer(ZONED_DATETIME_FORMATTER));

                objectMapper.registerModule(javaTimeModule);

                // Disable writing dates as timestamps (arrays)
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                // Configure to handle timezone information properly
                objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

                return objectMapper;
        }
}