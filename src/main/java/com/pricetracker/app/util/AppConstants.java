package com.pricetracker.app.util;

/**
 * Application Constants
 * 
 * Centralized storage for application-wide constant values.
 * Follow this pattern for all shared values across the application.
 */
public final class AppConstants {
    private AppConstants() {
        // Private constructor to prevent instantiation
    }

    // API versioning
    public static final String API_V1 = "/api/v1";

    // Server settings
    public static final int DEFAULT_SERVER_PORT = 8090;
    public static final String DEFAULT_CONTEXT_PATH = "/api/v1";

    // Pagination defaults
    public static final int MAX_PAGE_SIZE = 50;
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_PAGE_NUMBER = "0";

    // Locale settings
    public static final String DEFAULT_CURRENCY = "INR";
    public static final String DEFAULT_COUNTRY = "IN";
    public static final String DEFAULT_LOCALE = "en-IN"; // Indian English Locale
    public static final String DEFAULT_CURRENCY_SYMBOL = "₹"; // INR Currency Symbol

    // Date and time formats
    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yyyy"; // Date format for display
    public static final String DEFAULT_DATE_FORMAT_ISO = "yyyy-MM-dd"; // Date format for API
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"; // Simple datetime format without
                                                                                  // timezone

    // File upload limits
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final long MAX_REQUEST_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String[] SUPPORTED_IMAGE_FORMATS = { "image/jpeg", "image/png", "image/webp" };
    public static final String FILE_UPLOAD_DIR = "./uploads/images/";
    public static final String[] ALLOWED_FILE_EXTENSIONS = { "jpg", "jpeg", "png" };

    // Storage options
    public static final String STORAGE_TYPE_LOCAL = "local";
    public static final String STORAGE_TYPE_S3 = "s3";

    // Email settings
    public static final String EMAIL_HOST = "smtp.gmail.com";
    public static final int EMAIL_PORT = 587;

    // CORS settings
    public static final String[] DEFAULT_ALLOWED_ORIGINS = {
            "http://localhost:3000", // Customer frontend
            "http://localhost:3003" // Admin dashboard
    };
    public static final String[] DEFAULT_ALLOWED_METHODS = {
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
    };
    public static final String[] DEFAULT_ALLOWED_HEADERS = {
            "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"
    };
    public static final long DEFAULT_CORS_MAX_AGE = 3600;

    // Security constants
    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE = "JWT";

    // Default JWT settings (these can be overridden in application.yml)
    public static final long DEFAULT_JWT_EXPIRATION = 86400000; // 24 hours in milliseconds
    public static final long DEFAULT_REFRESH_TOKEN_EXPIRATION = 604800000; // 7 days in milliseconds
    public static final String REFRESH_TOKEN_STORAGE_DATABASE = "database";
    public static final String REFRESH_TOKEN_STORAGE_JWT = "jwt";

    // Role constants
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_MANAGER = "ROLE_MANAGER";

    // Database config
    public static final String DEFAULT_DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/electronics_store?useSSL=false&serverTimezone=UTC";
    public static final String DEFAULT_DB_DIALECT = "org.hibernate.dialect.MySQL8Dialect";

    // Cache settings
    public static final int DEFAULT_CACHE_EXPIRATION = 3600; // 1 hour in seconds

    // Endpoints
    public static final class Endpoints {
        public static final String AUTH = "/auth";
        public static final String LOGIN = "/login";
        public static final String REGISTER = "/register";
        public static final String REFRESH_TOKEN = "/refresh-token";
        public static final String LOGOUT = "/logout";

        public static final String PRODUCTS = "/products";
        public static final String CATEGORIES = "/categories";
        public static final String USERS = "/users";
        public static final String ORDERS = "/orders";
    }

    // Validation constraints
    public static final class ValidationConstraints {
        public static final int USERNAME_MIN_LENGTH = 4;
        public static final int USERNAME_MAX_LENGTH = 20;
        public static final int PASSWORD_MIN_LENGTH = 8;
        public static final int PASSWORD_MAX_LENGTH = 100;
        public static final int NAME_MIN_LENGTH = 2;
        public static final int NAME_MAX_LENGTH = 50;
        public static final int EMAIL_MAX_LENGTH = 100;
        public static final int PHONE_MIN_LENGTH = 10;
        public static final int PHONE_MAX_LENGTH = 15;
        public static final int ADDRESS_MAX_LENGTH = 200;
        public static final int TITLE_MIN_LENGTH = 3;
        public static final int TITLE_MAX_LENGTH = 100;
        public static final int DESCRIPTION_MAX_LENGTH = 1000;
        public static final String EMAIL_PATTERN = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        public static final String PHONE_PATTERN = "^[0-9]{10,15}$";
    }

    /**
     * Standard error messages used across the application
     */
    public static final class ErrorMessages {
        public static final String RESOURCE_NOT_FOUND = "Requested resource not found";
        public static final String UNAUTHORIZED = "You are not authorized to perform this action.";
        public static final String VALIDATION_FAILED = "Validation failed";
        public static final String GENERIC_ERROR = "An error occurred. Please try again.";
        public static final String NETWORK_ERROR = "Network error. Please check your connection.";
        public static final String INVALID_CREDENTIALS = "Invalid username or password";
        public static final String TOKEN_EXPIRED = "Your session has expired. Please login again.";
        public static final String INVALID_TOKEN = "Invalid token";
        public static final String EMAIL_EXISTS = "Email already exists";
        public static final String USERNAME_EXISTS = "Username already exists";
        public static final String PASSWORD_MISMATCH = "Passwords do not match";
        public static final String FORBIDDEN = "You don't have permission to access this resource";
        public static final String METHOD_NOT_ALLOWED = "This method is not allowed for this resource";
        public static final String FILE_TOO_LARGE = "File size exceeds the maximum allowed limit";
        public static final String INVALID_FILE_TYPE = "Invalid file type. Only JPG, JPEG, and PNG are allowed";
    }

    /**
     * Success messages used across the application
     */
    public static final class SuccessMessages {
        public static final String SAVE_SUCCESS = "Changes saved successfully.";
        public static final String DELETE_SUCCESS = "Item deleted successfully.";
        public static final String UPDATE_SUCCESS = "Item updated successfully.";
        public static final String CREATE_SUCCESS = "Item created successfully.";
        public static final String LOGIN_SUCCESS = "Login successful";
        public static final String LOGOUT_SUCCESS = "Logout successful";
        public static final String REGISTRATION_SUCCESS = "Registration successful";
        public static final String PASSWORD_RESET_SUCCESS = "Password reset successful";
        public static final String EMAIL_SENT = "Email sent successfully";
        public static final String FILE_UPLOAD_SUCCESS = "File uploaded successfully";
    }
}