# Price Tracker Application

A Java Spring Boot application for tracking product prices from online retailers.

## Features

- Track product prices from online retailers
- Automatic price checking at configurable intervals
- Price history tracking and visualization
- Email notifications when prices drop below your target price
- RESTful API for managing tracked products

## Technologies

- Java 17
- Spring Boot 3
- Spring Data JPA
- Spring Mail
- Thymeleaf
- Jsoup
- PostgreSQL
- Lombok
- Maven

## Setup and Configuration

### Prerequisites

- Java 17+
- Maven
- PostgreSQL

### Configuration

1. Clone the repository
2. Configure your database connection in `src/main/resources/application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/price_tracker_db
       username: your_username
       password: your_password
   ```

3. Configure email settings for notifications in `application.yml`:
   ```yaml
   spring:
     mail:
       host: your.smtp.server
       port: 587
       username: your_email@example.com
       password: your_password
       properties:
         mail.smtp.auth: true
         mail.smtp.starttls.enable: true
   ```

4. Configure scraping settings in `application.yml` (optional):
   ```yaml
   app:
     scheduling:
       checkRateMs: 3600000  # Check interval in milliseconds (1 hour)
     scraper:
       default-delay-ms: 1000  # Delay between scraping requests
       user-agent: "Your User Agent String"
   ```

### Building and Running

1. Build the application:
   ```
   mvn clean package
   ```

2. Run the application:
   ```
   java -jar target/price-tracker-app-0.0.1-SNAPSHOT.jar
   ```

   Or using Maven:
   ```
   mvn spring-boot:run
   ```

## API Endpoints

The application provides RESTful API endpoints for managing tracked products:

- `POST /api/v1/track` - Add a new product to track
- `GET /api/v1/track` - Get all tracked products
- `GET /api/v1/track/{id}` - Get a specific tracked product
- `PUT /api/v1/track/{id}` - Update a tracked product
- `DELETE /api/v1/track/{id}` - Delete a tracked product
- `GET /api/v1/track/product/{productId}/history` - Get price history for a product

## License

This project is licensed under the MIT License - see the LICENSE file for details. 