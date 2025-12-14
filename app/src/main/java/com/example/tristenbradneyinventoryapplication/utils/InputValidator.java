package com.example.tristenbradneyinventoryapplication.utils;

/**
 * InputValidator - Centralized input validation for security
 *
 * ENHANCEMENT THREE: Database Security and Input Validation
 *
 * This utility class provides comprehensive input validation to prevent:
 * - SQL injection through malformed inputs
 * - Data corruption from invalid values
 * - Application crashes from unexpected data
 *
 * All user inputs should be validated before database operations.
 *
 */
public class InputValidator {

    // Constants for validation rules
    private static final int MIN_ITEM_NAME_LENGTH = 1;
    private static final int MAX_ITEM_NAME_LENGTH = 100;
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MAX_PASSWORD_LENGTH = 50;
    private static final int MIN_QUANTITY = 0;
    private static final int MAX_QUANTITY = 1000000;
    private static final double MIN_PRICE = 0.0;
    private static final double MAX_PRICE = 1000000.0;

    /**
     * Validate item name
     *
     * Rules:
     * - Not null or empty
     * - Length between 1 and 100 characters
     * - No SQL injection patterns
     *
     * @param itemName The item name to validate
     * @return Error message if invalid, null if valid
     */
    public static String validateItemName(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            return "Item name cannot be empty";
        }

        String trimmed = itemName.trim();

        if (trimmed.length() < MIN_ITEM_NAME_LENGTH) {
            return "Item name is too short";
        }

        if (trimmed.length() > MAX_ITEM_NAME_LENGTH) {
            return "Item name is too long (max " + MAX_ITEM_NAME_LENGTH + " characters)";
        }

        // Check for potentially dangerous SQL patterns
        if (containsSqlInjectionPattern(trimmed)) {
            return "Item name contains invalid characters";
        }

        return null; // Valid
    }

    /**
     * Validate quantity
     *
     * Rules:
     * - Must be >= 0
     * - Must be reasonable (not millions)
     *
     * @param quantity The quantity to validate
     * @return Error message if invalid, null if valid
     */
    public static String validateQuantity(int quantity) {
        if (quantity < MIN_QUANTITY) {
            return "Quantity cannot be negative";
        }

        if (quantity > MAX_QUANTITY) {
            return "Quantity is unreasonably large";
        }

        return null; // Valid
    }

    /**
     * Validate price
     *
     * Rules:
     * - Must be >= 0
     * - Must be reasonable
     *
     * @param price The price to validate
     * @return Error message if invalid, null if valid
     */
    public static String validatePrice(double price) {
        if (price < MIN_PRICE) {
            return "Price cannot be negative";
        }

        if (price > MAX_PRICE) {
            return "Price is unreasonably large";
        }

        if (Double.isNaN(price) || Double.isInfinite(price)) {
            return "Price is invalid";
        }

        return null; // Valid
    }

    /**
     * Validate username
     *
     * Rules:
     * - Length between 3 and 20 characters
     * - Only alphanumeric and underscores
     * - No SQL injection patterns
     *
     * @param username The username to validate
     * @return Error message if invalid, null if valid
     */
    public static String validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "Username cannot be empty";
        }

        String trimmed = username.trim();

        if (trimmed.length() < MIN_USERNAME_LENGTH) {
            return "Username must be at least " + MIN_USERNAME_LENGTH + " characters";
        }

        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            return "Username cannot exceed " + MAX_USERNAME_LENGTH + " characters";
        }

        // Only allow alphanumeric and underscores
        if (!trimmed.matches("^[a-zA-Z0-9_]+$")) {
            return "Username can only contain letters, numbers, and underscores";
        }

        return null; // Valid
    }

    /**
     * Validate password
     *
     * Rules:
     * - Length between 4 and 50 characters
     * - Not empty or whitespace only
     *
     * @param password The password to validate
     * @return Error message if invalid, null if valid
     */
    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters";
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            return "Password cannot exceed " + MAX_PASSWORD_LENGTH + " characters";
        }

        return null; // Valid
    }

    /**
     * Check for common SQL injection patterns
     *
     * This is a basic check - Room's parameterized queries are the primary defense
     *
     * @param input The input string to check
     * @return true if suspicious patterns detected, false otherwise
     */
    private static boolean containsSqlInjectionPattern(String input) {
        if (input == null) {
            return false;
        }

        String lower = input.toLowerCase();

        // Check for common SQL injection patterns
        String[] dangerousPatterns = {
                "';", "--", "/*", "*/", "xp_", "sp_",
                "exec", "execute", "drop", "delete",
                "insert", "update", "union", "select"
        };

        for (String pattern : dangerousPatterns) {
            if (lower.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sanitize input by removing potentially dangerous characters
     *
     * Note: This is a defense-in-depth measure. Parameterized queries
     * are the primary protection against SQL injection.
     *
     * @param input The input string to sanitize
     * @return Sanitized string
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }

        // Remove potentially dangerous characters
        return input
                .replaceAll("[';\"\\\\]", "") // Remove quotes and backslashes
                .replaceAll("--", "")          // Remove SQL comments
                .replaceAll("/\\*", "")        // Remove block comment start
                .replaceAll("\\*/", "")        // Remove block comment end
                .trim();
    }

    /**
     * Validate complete inventory item
     *
     * @param itemName Item name
     * @param quantity Quantity
     * @param price Price
     * @return Error message if invalid, null if all valid
     */
    public static String validateInventoryItem(String itemName, int quantity, double price) {
        String nameError = validateItemName(itemName);
        if (nameError != null) {
            return nameError;
        }

        String quantityError = validateQuantity(quantity);
        if (quantityError != null) {
            return quantityError;
        }

        String priceError = validatePrice(price);
        if (priceError != null) {
            return priceError;
        }

        return null; // All valid
    }
}
