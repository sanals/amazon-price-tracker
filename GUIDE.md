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

### Handling Amazon Shortened URLs

If you're tracking products from Amazon with shortened URLs (like amzn.in or a.co), you may encounter HTTP 500 errors:

```
HTTP error 500 for URL: https://amzn.in/d/bA9iHdh
```

This occurs because:

1. **URL Redirection Issues**: Shortened URLs need redirection handling
2. **Anti-Scraping Measures**: Amazon detects and blocks automated requests
3. **User-Agent Detection**: Amazon rejects requests that identify as bots

The application has been enhanced to handle these issues by:

1. **URL Expansion**: Automatically expanding shortened URLs before scraping
2. **Browser-Like Headers**: Using realistic browser headers and rotating user agents
3. **Random Delays**: Adding random delays between requests to appear more human-like

Best practices for tracking Amazon products:

1. **Use Full URLs**: When possible, use full Amazon product URLs instead of shortened ones
   ```
   ✅ https://www.amazon.in/Apple-iPhone-13-128GB-Midnight/dp/B09G9HD6PD
   ❌ https://amzn.in/d/bA9iHdh
   ```

2. **Adjust Scraping Frequency**: Set the `app.scheduling.checkRateMs` to a reasonable value (at least 1 hour)
   ```yaml
   app:
     scheduling:
       checkRateMs: 3600000  # 1 hour in milliseconds
   ```

3. **Test URLs First**: Use the test endpoint before adding URLs to track
   ```
   GET /api/v1/test/scrape?url=https://www.amazon.in/Apple-iPhone-13-128GB-Midnight/dp/B09G9HD6PD
   ```

4. **Use Regional Domains**: Ensure you're using the correct regional domain (amazon.in for India, amazon.com for US)

For Amazon India specifically, you can use the specialized test endpoint:
```
GET /api/v1/test/scrape-amazon-india?url=https://www.amazon.in/product
```

If you're still experiencing issues, try using the local HTML file testing approach:
1. Save the product page HTML locally
2. Test it with: `GET /api/v1/test/scrape-amazon-india?useFile=true`