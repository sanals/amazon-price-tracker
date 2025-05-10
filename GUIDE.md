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