# Price Tracker API Usage Guide

## User Authentication

### 1. Register a New User
**Endpoint:** `POST /api/v1/auth/register`
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Password123!"
}
```

**Alternative User Registration:**
**Endpoint:** `POST /api/v1/users/create`
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Password123!",
  "role": "USER"
}
```
**Note:** If you encounter a "null value in column 'password_hash' violates not-null constraint" error, there's a mismatch between the entity field and database column. Check if your User entity has a field named `password` but the database expects `password_hash`.

### 2. Login
**Endpoint:** `POST /api/v1/auth/login`
```json
{
  "username": "testuser",
  "password": "Password123!"
}
```
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Login successful",
  "data": {
    "token": "your_jwt_token",
    "refreshToken": "your_refresh_token",
    "username": "testuser",
    "role": "USER"
  },
  "timestamp": "2023-06-01T12:34:56.789Z"
}
```

**Note:** 
- Make sure to register a user first before attempting to login.
- The application now uses a properly secured JWT token for authentication.
- After login, use the returned token in the Authorization header for all protected endpoints.
  
**Previous issues (now fixed):**
1. ~~StackOverflowError~~ - Fixed by removing circular references in User entity methods
2. ~~ClassCastException~~ - Fixed by properly retrieving the user from repository instead of casting
3. ~~WeakKeyException~~ - Fixed by using a sufficiently long JWT secret key (at least 256 bits)

## Product Tracking

### 3. Add a Product to Track
**Endpoint:** `POST /api/v1/track`
```json
{
  "productUrl": "https://www.amazon.com/dp/B08F5F1TN4",
  "desiredPrice": 449.99
}
```

### 4. Get All Tracked Products
**Endpoint:** `GET /api/v1/track?page=0&size=10&sort=createdAt,desc`
**Headers:** `Authorization: Bearer your_jwt_token`
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Successfully retrieved tracked products",
  "data": {
    "content": [
      {
        "id": 1,
        "product": {
          "id": 1,
          "productUrl": "https://www.amazon.com/dp/B08F5F1TN4",
          "name": "PlayStation 5 Console",
          "imageUrl": "https://m.media-amazon.com/images/I/51051FiD9UL._AC_SX679_.jpg",
          "lastCheckedPrice": 499.99
        },
        "desiredPrice": 449.99,
        "notificationEnabled": true,
        "createdAt": "2023-06-01T10:30:00.000Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "size": 10,
    "number": 0,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "numberOfElements": 1,
    "first": true,
    "empty": false
  },
  "timestamp": "2023-06-01T12:34:56.789Z"
}
```

### 5. Get Details of a Specific Tracked Product
**Endpoint:** `GET /api/v1/track/{trackedProductId}`
**Headers:** `Authorization: Bearer your_jwt_token`

### 6. Update a Tracked Product
**Endpoint:** `PUT /api/v1/track/{trackedProductId}`
**Headers:** `Authorization: Bearer your_jwt_token`
```json
{
  "desiredPrice": 429.99,
  "notificationEnabled": true
}
```

### 7. Stop Tracking a Product
**Endpoint:** `DELETE /api/v1/track/{trackedProductId}`
**Headers:** `Authorization: Bearer your_jwt_token`

## Price History

### 8. Get Price History for a Product
**Endpoint:** `GET /api/v1/track/product/{productId}/history?page=0&size=20&sort=timestamp,desc`
**Headers:** `Authorization: Bearer your_jwt_token`
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Successfully retrieved price history",
  "data": {
    "content": [
      {
        "id": 3,
        "price": 479.99,
        "timestamp": "2023-06-01T00:00:00.000Z"
      },
      {
        "id": 2,
        "price": 499.99,
        "timestamp": "2023-05-29T00:00:00.000Z"
      },
      {
        "id": 1,
        "price": 519.99,
        "timestamp": "2023-05-25T00:00:00.000Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 3,
    "totalPages": 1,
    "last": true,
    "size": 20,
    "number": 0,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "numberOfElements": 3,
    "first": true,
    "empty": false
  },
  "timestamp": "2023-06-01T12:34:56.789Z"
}

**Important Note About Sorting:**
When using the sort parameter, you must specify the property name correctly. For example:
- ✅ Correct: `sort=timestamp,desc`
- ❌ Incorrect: `sort=["string"]` or other JSON-formatted values

If you use invalid sort values like `sort=["string"]`, you'll receive the following error:
```
PropertyReferenceException: No property '["string"]' found for type 'PriceHistory'
```

This happens because Spring Data expects simple property names (like "timestamp") rather than JSON arrays or objects in the sort parameter.

## Account Management

### 9. Change Password
**Endpoint:** `POST /api/v1/auth/change-password`
```json
{
  "currentPassword": "Password123!",
  "newPassword": "NewPassword456!",
  "confirmPassword": "NewPassword456!"
}
```

### 10. Reset Password
**Step 1:** Request password reset email
**Endpoint:** `POST /api/v1/auth/forgot-password?email=test@example.com`

**Step 2:** Use the token from the email to reset the password
**Endpoint:** `POST /api/v1/auth/reset-password?token={reset_token}&newPassword=NewPassword789!`

### 11. Logout
**Endpoint:** `POST /api/v1/auth/logout`
**Headers:**
```
Authorization: Bearer {your_jwt_token}
```

## How Price Checking Works

### Automated Price Checking
The application automatically checks the current prices of tracked products on a scheduled basis:

1. A scheduled task runs every hour (configurable in application.yml with `app.scheduling.checkRateMs`)
2. For each product in the database, the system:
   - Scrapes the current price using Jsoup from the product URL
   - If the price has changed, updates the product's `lastCheckedPrice` and creates a new `PriceHistory` record
   - If the price has dropped below a user's desired price, sends a notification

### Price Scraping Process
The application uses the Jsoup library to scrape prices from retail websites:

1. The `JsoupScraperService` connects to the product URL with a proper user agent
2. It uses CSS selectors to look for price elements (e.g., "#price, .price, [data-price]")
3. The price text is extracted, cleaned (removing currency symbols, etc.), and converted to a BigDecimal

### Notification System
When a price drops below a user's desired price, the system sends notifications:

1. The system compares the current price with the desired price set by each user tracking the product
2. If the current price is less than or equal to the desired price, a notification is triggered
3. A cooldown period prevents sending too many notifications (configurable via `app.notification.cooldown-hours`)
4. Email notifications are sent using Spring Mail with Thymeleaf templates

### Testing the System
To force a price check rather than waiting for the scheduler:

1. Update a product's URL in the database to a different product with a lower price
2. The next scheduled run will detect the price change and send a notification
3. You can monitor the price history using the API endpoint for price history

## Notes
- Replace `{your_jwt_token}` with the actual JWT token received during login.
- Replace `{trackedProductId}` and `{productId}` with the actual IDs of the tracked products.
- Ensure to handle responses and errors appropriately in your application.

## Testing and Troubleshooting

### Testing Price Scraping
To test if the price scraping functionality is working correctly, use the test endpoint:

**Endpoint:** `GET /api/v1/test/scrape?url=PRODUCT_URL`
**Headers:** `Authorization: Bearer your_jwt_token`

Replace `PRODUCT_URL` with an actual product URL you want to test (make sure to URL-encode it).

Example:
```
GET /api/v1/test/scrape?url=https%3A%2F%2Fwww.amazon.com%2Fdp%2FB08F5F1TN4
```

This will return the scraped product details:
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Scrape test completed",
  "data": {
    "priceResult": 499.99,
    "name": "PlayStation 5 Console",
    "imageUrl": "https://m.media-amazon.com/images/I/51051FiD9UL._AC_SX679_.jpg",
    "price": 499.99
  },
  "timestamp": "2023-06-01T12:34:56.789Z"
}
```

### Troubleshooting Scraping Issues

If the price scraping is not working as expected, check the following:

1. **Database Configuration**:
   - Ensure database credentials in application.yml are correct
   - Verify the database and tables exist

2. **Product URLs**:
   - Make sure URLs in the database are valid and accessible
   - Check if the scraper can access the URLs (some sites block scrapers)

3. **CSS Selectors**:
   - The application uses general CSS selectors to find prices on various retail websites
   - If a specific site isn't working, you may need to add site-specific selectors

4. **Price History Records**:
   - Price history is only created when a price change is detected
   - Check if any records exist in the price_history table:
     ```sql
     SELECT * FROM price_history ORDER BY timestamp DESC LIMIT 10;
     ```

5. **Scheduler Configuration**:
   - The price check scheduler runs every 6 seconds by default (configured in application.yml)
   - You can adjust this with the `app.scheduling.checkRateMs` property

### Forcing a Price Update

If you want to force a price update for testing:

1. Manually update a product URL in the database to a different URL with a known different price
2. Wait for the scheduler to run (by default every 6 seconds)
3. Check the price_history table for new records

### Viewing Price History

As mentioned in the previous section, use the price history endpoint to see the recorded price changes:
```
GET /api/v1/track/product/{productId}/history?page=0&size=20&sort=timestamp,desc
```

## Troubleshooting Common Price Tracking Issues

### Database Connection Issues

If you're not seeing price history being recorded or the application fails to start:

1. **Database credentials**: Verify your database credentials in `application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/price_tracker_db
       username: postgres
       password: root
       driver-class-name: org.postgresql.Driver
   ```

2. **Database existence**: Make sure the `price_tracker_db` database exists:
   ```sql
   CREATE DATABASE price_tracker_db;
   ```

3. **Connection test**: Test PostgreSQL connection directly:
   ```
   psql -U postgres -d price_tracker_db -h localhost -W
   ```

4. **Security settings**: Ensure your PostgreSQL `pg_hba.conf` allows password authentication for local connections.

### Price Scraping Not Working

If prices aren't being scraped or updated correctly:

1. **Test specific URLs**: Use the test endpoint to check if scraping works for specific URLs:
   ```
   GET /api/v1/test/scrape?url=https://www.amazon.com/product
   ```

2. **CSS selectors**: The application uses general CSS selectors to find prices which may not work for all sites. Check `JsoupScraperService.java` and see if specific selectors for your target website are missing.

3. **Site blocking**: Some websites actively block scrapers. Ensure `app.scraper.user-agent` in `application.yml` is set to something reasonable.

4. **Website structure changes**: Retail websites frequently update their HTML structure. You may need to update the selectors in the scraper service.

5. **Response content**: Use the browser's "View Source" feature to see if the price elements are actually in the HTML or if they're loaded via JavaScript (which Jsoup can't handle).

### Price History Not Being Updated

If price scraping works but history isn't being recorded:

1. **Scheduler running**: Check if the scheduler is running properly - it's configured to run every 6 seconds by default (30000ms in application.yml but can be overridden):
   ```yaml
   app:
     scheduling:
       checkRateMs: 30000  # milliseconds
   ```

2. **Verify price changes**: Price history is only recorded when a price change is detected. If prices remain the same, no new records are created.

3. **Test scheduler manually**: Use the test endpoint to manually trigger the scheduler:
   ```
   POST /api/v1/test/run-scheduler
   ```

4. **Check database tables**: Query your database to see if price history records exist:
   ```sql
   SELECT * FROM price_history ORDER BY timestamp DESC LIMIT 10;
   SELECT * FROM products ORDER BY updated_at DESC LIMIT 10;
   ```

5. **Transaction issues**: If using PostgreSQL, ensure the database user has proper rights to perform transaction operations.

### Amazon India (and other specific sites) Scraping

For sites with complex or regularly changing structures like Amazon:

1. **Use specialized strategies**: The application includes an `AmazonScraperStrategy` specifically tailored for Amazon sites. For other specific sites, consider implementing similar strategies.

2. **Regular expression patterns**: The Amazon strategy uses regex patterns like:
   ```java
   private static final Pattern PRICE_PATTERN = Pattern.compile("(?:₹|Rs\\.?|INR)?\\s*([\\d,]+(?:\\.\\d+)?)");
   ```
   This allows it to extract prices in various formats (₹4,599.00, ₹4,599, 4599).

3. **Custom selectors**: For Amazon, specific selectors are defined:
   ```java
   private static final List<String> PRICE_SELECTORS = Arrays.asList(
       ".priceToPay .a-offscreen",
       ".apexPriceToPay .a-offscreen",
       // ... more selectors
   );
   ```

4. **Fallback methods**: The strategy employs multiple fallback methods, trying different approaches if primary selectors fail.

### Recommended Changes for Better Scraping Performance

1. **Frequency adjustment**: Consider changing the scheduler frequency to be less aggressive (every 1-6 hours instead of every 6 seconds) to avoid being blocked by websites:
   ```yaml
   app:
     scheduling:
       checkRateMs: 3600000  # 1 hour in milliseconds
   ```

2. **Site-specific strategies**: Implement more site-specific strategies for retail websites you frequently track.

3. **Better error handling**: Enhance error logging to pinpoint exactly which part of the scraping process is failing.

4. **HTTP proxy rotation**: For production use, consider implementing proxy rotation to avoid IP blocks for frequent requests.

5. **Storage optimization**: Implement logic to store price history with reduced frequency over time (e.g., daily averages for older data).

### Enhanced Amazon India Scraping Support

### Handling Amazon India Shortened URLs

Amazon India frequently uses shortened URLs (like `https://amzn.in/d/4BQSnoC`) which present unique challenges:

1. These URLs require redirection expansion
2. They often trigger CAPTCHA verification mechanisms
3. The price structure on Amazon India sites can be different from other regions

To address these challenges, we've implemented:

1. **URL Expansion**: The application automatically expands shortened URLs before scraping
   ```
   # Example of URL expansion
   https://amzn.in/d/4BQSnoC -> https://www.amazon.in/product/dp/PRODUCTID
   ```

2. **CAPTCHA Detection**: Enhanced detection of CAPTCHA verification pages to avoid incorrect parsing
   ```
   # Signs of CAPTCHA pages
   - Title containing "Robot Check" or "Enter the characters"
   - Images with src containing "captcha"
   - Text like "Type the characters you see in this image"
   ```

3. **Specialized Price Selectors**: Multiple selectors focused on Amazon India's HTML structure
   ```
   # Primary Amazon India price selectors
   .priceToPay .a-offscreen
   .apexPriceToPay .a-offscreen
   #corePrice_feature_div .a-price .a-offscreen
   ```

4. **Robust Price Parsing**: Handling various Indian Rupee formats (₹4,599.00, ₹4,599, ₹ 4599)
   ```
   # Using regex pattern: (?:₹|Rs\.?|INR)?\s*([\d,]+(?:\.\d+)?)
   ```

### Testing Amazon India URLs

For optimal testing with Amazon India sites:

1. **Use the Specialized Testing Endpoint**:
   ```
   GET /api/v1/test/scrape-amazon-shortened?url=https://amzn.in/d/4BQSnoC
   ```
   This endpoint provides detailed diagnostic information about:
   - URL expansion attempts
   - CAPTCHA detection
   - Available price elements
   - Extraction results

2. **Avoid Triggering CAPTCHA**:
   - Don't make requests too frequently (increase the scheduler interval)
   - Consider using different user agents for each request
   - If you encounter persistent CAPTCHA problems, try using the full URL format
   
3. **Debugging Price Extraction**:
   The enhanced response includes which selectors matched, helping you understand why a particular price was chosen.

### Common Issues and Solutions

1. **Getting CAPTCHA Pages**:
   - **Problem**: `Detected CAPTCHA page with following signals: title=true, image=true, text=true`
   - **Solution**: Reduce scraping frequency, use full URLs, rotate IP addresses, or use proxy services

2. **Incorrect Prices (Too Low)**: 
   - **Problem**: Scraper picking up related product prices (₹225.00) that are much lower than the main product
   - **Solution**: Price validation with minimum thresholds and text length limits

3. **Inconsistent Prices Between Scrapes**:
   - **Problem**: Same URL gives different prices on different scrapes
   - **Solution**: The improved scraper prioritizes price containers in a consistent order

4. **Shortened URL Expansion Fails**:
   - **Problem**: `Error: expandedUrl not available`
   - **Solution**: Try the full URL format directly or check network connectivity

For further troubleshooting, use the enhanced test endpoint to get a complete view of the scraping process.

## Enhanced Scraper Architecture

### Strategy Pattern Implementation

The scraper system has been completely redesigned to use a proper Strategy Pattern, allowing for:

1. Multiple website-specific scraper implementations 
2. A common interface with standard methods
3. Automatic detection of which scraper to use for a given URL
4. Extension by adding new strategies without modifying existing code

Key components of the new architecture:

#### 1. ScraperStrategy Interface

The base interface that all scraper strategies must implement:

```java
public interface ScraperStrategy {
    boolean canHandle(String url);
    Optional<BigDecimal> extractPrice(Document doc);
    Optional<String> extractName(Document doc);
    Optional<String> extractImageUrl(Document doc);
    Optional<ProductDetails> scrapeProductDetails(Document doc);
    boolean isCaptchaPage(Document doc);
    String expandShortenedUrl(String shortenedUrl);
}
```

#### 2. BaseScraperStrategy Abstract Class

A common base implementation with shared utilities:

```java
public abstract class BaseScraperStrategy implements ScraperStrategy {
    // Common utilities for price parsing, URL expansion, CAPTCHA detection
    protected Optional<BigDecimal> parsePrice(String text);
    protected Optional<BigDecimal> extractPriceWithSelectors(Document doc, String[] selectors);
    protected boolean isLikelyMainPrice(String text);
}
```

#### 3. Website-Specific Strategies

Specialized implementations for different retail websites:

```java
@Component
public class AmazonScraperStrategy extends BaseScraperStrategy {
    // Amazon-specific selectors and parsing logic
    // Special handling for Amazon India prices
    // CAPTCHA detection for Amazon
}
```

#### 4. Scraper Service

Manages all strategies and delegates scraping to the appropriate one:

```java
@Service
public class JsoupScraperService implements ScraperService {
    private final List<ScraperStrategy> scraperStrategies;
    
    // Register strategies via constructor injection
    public JsoupScraperService(AmazonScraperStrategy amazonStrategy) {
        registerStrategy(amazonStrategy);
    }
    
    // Find the right strategy for a URL
    public Optional<ScraperStrategy> findStrategyForUrl(String url) {
        for (ScraperStrategy strategy : scraperStrategies) {
            if (strategy.canHandle(url)) {
                return Optional.of(strategy);
            }
        }
        return Optional.empty();
    }
    
    // Main scraping methods delegate to appropriate strategy
    public Optional<BigDecimal> scrapePrice(String url) {
        Document doc = fetchDocument(url);
        Optional<ScraperStrategy> strategy = findStrategyForUrl(url);
        
        if (strategy.isPresent()) {
            return strategy.get().extractPrice(doc);
        }
        
        // Fall back to generic approach if no specific strategy
        return extractGenericPrice(doc);
    }
}
```

### Benefits of the New Architecture

1. **Extensibility**: Add new retailer support by simply creating a new strategy class
2. **Maintainability**: Each retailer's logic is isolated in its own class
3. **Testability**: Each strategy can be tested independently
4. **Robustness**: Better handling of various edge cases and CAPTCHA detection

### Amazon India Enhanced Support

The AmazonScraperStrategy now includes specialized handling for Amazon India pages:

1. **Detect India-specific pages** using domain (.in) and content checks
2. **Special handling for Indian Rupee format** (₹1,449.00, ₹1,449, etc.)
3. **Site-specific selectors** targeting the unique structure of Amazon India product pages
4. **Power adapter price detection** with specialized selectors for the examples provided

### Testing Endpoints

Several test endpoints are available to evaluate the new architecture:

- `/api/v1/test/test-power-adapter`: Test the power adapter URL specifically
- `/api/v1/test/compare-scrapers`: Compare all registered scrapers for a given URL
- `/api/v1/test/scrape`: General scraping test
- `/api/v1/test/save-product`: Save a product to the database
- `/api/v1/test/run-scheduler`: Manually trigger the scheduler
- `/api/v1/test/latest-prices`: View recent price history