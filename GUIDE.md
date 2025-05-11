# Price Tracker API Usage Guide

## User Authentication

### 1. Register a New User
**Endpoint:** `POST /api/v1/users/create`
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Password123!",
  "role": "USER"
}
```
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 201,
  "message": "User created successfully",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "role": "USER",
    "status": "ACTIVE"
  },
  "timestamp": "2023-06-01T12:34:56.789Z"
}
```

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
- The application uses a properly secured JWT token for authentication.
- After login, use the returned token in the Authorization header for all protected endpoints.

## Product Tracking

### 3. Add a Product to Track
**Endpoint:** `POST /api/v1/track`
**Headers:** `Authorization: Bearer your_jwt_token`
**Query Parameters:** `userId=1` (The ID of the user tracking the product)
```json
{
  "productUrl": "https://www.amazon.com/dp/B08F5F1TN4",
  "desiredPrice": 449.99
}
```
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 201,
  "message": "Product tracking added successfully",
  "data": {
    "id": 1,
    "product": {
      "id": 1,
      "productUrl": "https://www.amazon.com/dp/B08F5F1TN4",
      "name": "PlayStation 5 Console",
      "imageUrl": "https://m.media-amazon.com/images/I/51051FiD9UL._AC_SX679_.jpg",
      "lastCheckedPrice": 499.99,
      "createdAt": "2023-06-01T10:30:00.000Z",
      "updatedAt": "2023-06-01T10:30:00.000Z"
    },
    "desiredPrice": 449.99,
    "notificationEnabled": true,
    "createdAt": "2023-06-01T10:30:00.000Z",
    "updatedAt": "2023-06-01T10:30:00.000Z"
  },
  "timestamp": "2023-06-01T12:34:56.789Z"
}
```

### 4. Get All Tracked Products for a User
**Endpoint:** `GET /api/v1/track/user/{userId}`
**Headers:** `Authorization: Bearer your_jwt_token`
**Query Parameters:** `page=0&size=10&sort=createdAt,desc`
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Tracked products retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "product": {
          "id": 1,
          "productUrl": "https://www.amazon.com/dp/B08F5F1TN4",
          "name": "PlayStation 5 Console",
          "imageUrl": "https://m.media-amazon.com/images/I/51051FiD9UL._AC_SX679_.jpg",
          "lastCheckedPrice": 499.99,
          "createdAt": "2023-06-01T10:30:00.000Z",
          "updatedAt": "2023-06-01T10:30:00.000Z"
        },
        "desiredPrice": 449.99,
        "notificationEnabled": true,
        "createdAt": "2023-06-01T10:30:00.000Z",
        "updatedAt": "2023-06-01T10:30:00.000Z"
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
**Endpoint:** `GET /api/v1/track/{trackedProductId}/user/{userId}`
**Headers:** `Authorization: Bearer your_jwt_token`
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Tracked product retrieved successfully",
  "data": {
    "id": 1,
    "product": {
      "id": 1,
      "productUrl": "https://www.amazon.com/dp/B08F5F1TN4",
      "name": "PlayStation 5 Console",
      "imageUrl": "https://m.media-amazon.com/images/I/51051FiD9UL._AC_SX679_.jpg",
      "lastCheckedPrice": 499.99,
      "createdAt": "2023-06-01T10:30:00.000Z",
      "updatedAt": "2023-06-01T10:30:00.000Z"
    },
    "desiredPrice": 449.99,
    "notificationEnabled": true,
    "createdAt": "2023-06-01T10:30:00.000Z",
    "updatedAt": "2023-06-01T10:30:00.000Z"
  },
  "timestamp": "2023-06-01T12:34:56.789Z"
}
```

### 6. Update a Tracked Product
**Endpoint:** `PUT /api/v1/track/{trackedProductId}/user/{userId}`
**Headers:** `Authorization: Bearer your_jwt_token`
```json
{
  "desiredPrice": 429.99,
  "notificationEnabled": true
}
```
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Tracked product updated successfully",
  "data": {
    "id": 1,
    "product": {
      "id": 1,
      "productUrl": "https://www.amazon.com/dp/B08F5F1TN4",
      "name": "PlayStation 5 Console",
      "imageUrl": "https://m.media-amazon.com/images/I/51051FiD9UL._AC_SX679_.jpg",
      "lastCheckedPrice": 499.99,
      "createdAt": "2023-06-01T10:30:00.000Z",
      "updatedAt": "2023-06-01T10:30:00.000Z"
    },
    "desiredPrice": 429.99,
    "notificationEnabled": true,
    "createdAt": "2023-06-01T10:30:00.000Z",
    "updatedAt": "2023-06-01T12:45:00.000Z"
  },
  "timestamp": "2023-06-01T12:45:00.789Z"
}
```

### 7. Stop Tracking a Product
**Endpoint:** `DELETE /api/v1/track/{trackedProductId}/user/{userId}`
**Headers:** `Authorization: Bearer your_jwt_token`
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 204,
  "message": "Tracked product deleted successfully",
  "data": null,
  "timestamp": "2023-06-01T12:34:56.789Z"
}
```

## Price History

### 8. Get Price History for a Product
**Endpoint:** `GET /api/v1/track/product/{productId}/history`
**Headers:** `Authorization: Bearer your_jwt_token`
**Query Parameters:** `page=0&size=20&sort=timestamp,desc`
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Price history retrieved successfully",
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
```

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
**Headers:** `Authorization: Bearer your_jwt_token`
```json
{
  "currentPassword": "Password123!",
  "newPassword": "NewPassword456!",
  "confirmPassword": "NewPassword456!"
}
```
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Password changed successfully",
  "data": null,
  "timestamp": "2023-06-01T12:34:56.789Z"
}
```

### 10. Reset Password
**Step 1:** Request password reset email
**Endpoint:** `POST /api/v1/auth/forgot-password?email=test@example.com`
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Password reset email sent successfully",
  "data": null,
  "timestamp": "2023-06-01T12:34:56.789Z"
}
```

**Step 2:** Use the token from the email to reset the password
**Endpoint:** `POST /api/v1/auth/reset-password?token={reset_token}&newPassword=NewPassword789!`
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Password reset successfully",
  "data": null,
  "timestamp": "2023-06-01T12:34:56.789Z"
}
```

### 11. Logout
**Endpoint:** `POST /api/v1/auth/logout`
**Headers:** `Authorization: Bearer your_jwt_token`
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Logged out successfully",
  "data": null,
  "timestamp": "2023-06-01T12:34:56.789Z"
}
```

## Health Check

### 12. API Health Check
**Endpoint:** `GET /api/v1/health`
**Response:**
```json
{
  "status": "SUCCESS",
  "code": 200,
  "message": "Health check successful",
  "data": "Price Tracker API is operational",
  "timestamp": "2023-06-01T12:34:56.789Z"
}
```

## Testing Endpoints

The application provides several testing endpoints for debugging and verification purposes:

### 13. Test Price Scraping
**Endpoint:** `GET /api/v1/test/scrape?url=PRODUCT_URL`
**Headers:** `Authorization: Bearer your_jwt_token`

Replace `PRODUCT_URL` with an actual product URL you want to test (make sure to URL-encode it).

Example:
```
GET /api/v1/test/scrape?url=https%3A%2F%2Fwww.amazon.com%2Fdp%2FB08F5F1TN4
```

### 14. Test Amazon India Scraping
**Endpoint:** `GET /api/v1/test/scrape-amazon-india?url=PRODUCT_URL`
**Headers:** `Authorization: Bearer your_jwt_token`

### 15. Test Shortened Amazon URLs
**Endpoint:** `GET /api/v1/test/scrape-amazon-shortened?url=SHORTENED_URL`
**Headers:** `Authorization: Bearer your_jwt_token`

Example:
```
GET /api/v1/test/scrape-amazon-shortened?url=https://amzn.in/d/gCtpi9n
```

### 16. Test Amazon Power Adapter Scraping
**Endpoint:** `GET /api/v1/test/scrape-power-adapter?url=PRODUCT_URL`
**Headers:** `Authorization: Bearer your_jwt_token`

### 17. Compare Different Scrapers
**Endpoint:** `GET /api/v1/test/compare-scrapers?url=PRODUCT_URL`
**Headers:** `Authorization: Bearer your_jwt_token`

### 18. Manually Save a Product
**Endpoint:** `POST /api/v1/test/save-product?url=PRODUCT_URL`
**Headers:** `Authorization: Bearer your_jwt_token`

### 19. Manually Run the Price Check Scheduler
**Endpoint:** `POST /api/v1/test/run-scheduler`
**Headers:** `Authorization: Bearer your_jwt_token`

### 20. View Latest Prices
**Endpoint:** `GET /api/v1/test/latest-prices?limit=10`
**Headers:** `Authorization: Bearer your_jwt_token`

## How Price Checking Works

### Automated Price Checking
The application automatically checks the current prices of tracked products on a scheduled basis:

1. A scheduled task runs periodically (configurable in application.yml with `app.scheduling.checkRateMs`)
2. For each product in the database, the system:
   - Scrapes the current price using Jsoup from the product URL
   - If the price has changed, updates the product's `lastCheckedPrice` and creates a new `PriceHistory` record
   - If the price has dropped below a user's desired price, sends a notification

### Price Scraping Process
The application uses a strategy pattern with Jsoup to scrape prices from retail websites:

1. The `JsoupScraperService` determines the appropriate scraper strategy for the URL
2. The strategy connects to the product URL with a proper user agent
3. It uses site-specific CSS selectors to look for price elements
4. The price text is extracted, cleaned (removing currency symbols, etc.), and converted to a BigDecimal

### Notification System
When a price drops below a user's desired price, the system sends notifications:

1. The system compares the current price with the desired price set by each user tracking the product
2. If the current price is less than or equal to the desired price, a notification is triggered
3. A cooldown period prevents sending too many notifications (configurable via `app.notification.cooldown-hours`)
4. Email notifications are sent using Spring Mail with templates

## Notes
- Replace `{your_jwt_token}` with the actual JWT token received during login.
- Replace `{userId}`, `{trackedProductId}`, and `{productId}` with the actual IDs.
- All API endpoints are prefixed with `/api/v1`. This prefix should be added to all paths in this guide.
- Ensure to handle responses and errors appropriately in your application.

## Enhanced Scraper Architecture

The scraper system uses a Strategy Pattern, allowing for:

1. Multiple website-specific scraper implementations 
2. A common interface with standard methods
3. Automatic detection of which scraper to use for a given URL
4. Extension by adding new strategies without modifying existing code

### Amazon India Enhanced Support

The AmazonScraperStrategy includes specialized handling for Amazon India pages:

1. **Detect India-specific pages** using domain (.in) and content checks
2. **Special handling for Indian Rupee format** (₹1,449.00, ₹1,449, etc.)
3. **Site-specific selectors** targeting the unique structure of Amazon India product pages
4. **URL expansion** for shortened Amazon links (amzn.in)