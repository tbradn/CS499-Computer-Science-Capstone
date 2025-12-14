package com.example.tristenbradneyinventoryapplication;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

/**
 * LoginActivity handles user authentication and new account creation.
 * This activity integrates with the MVVM architecture to provide secure login functionality.
 *
 * UPDATED FOR ENHANCEMENT ONE: Now uses ViewModel instead of DatabaseHelper
 */
public class LoginActivity extends Activity {

    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private Button createAccountButton;

    // ViewModel instance (replaces DatabaseHelper)
    private InventoryViewModel viewModel;

    // SharedPreferences for session management
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "InventoryAppPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeComponents();
        checkExistingSession();
        setupClickListeners();
    }

    /**
     * Initialize all UI components and helper classes.
     */
    private void initializeComponents() {
        // Initialize UI components
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        createAccountButton = findViewById(R.id.create_account_button);

        // Initialize ViewModel without ViewModelProvider
        // Use Application context instead
        viewModel = new InventoryViewModel(getApplication());

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    /**
     * Check if user is already logged in and skip login if necessary.
     */
    private void checkExistingSession() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);

        if (isLoggedIn) {
            long userId = sharedPreferences.getLong(KEY_USER_ID, -1);
            String username = sharedPreferences.getString(KEY_USERNAME, "");

            if (userId != -1 && !username.isEmpty()) {

                // User is already logged in, skip to permissions screen
                Toast.makeText(this, "Welcome back, " + username + "!", Toast.LENGTH_SHORT).show();
                proceedToPermissions();
                return;
            }
        }

        // Clear any invalid session data
        clearSession();
    }

    /**
     * Set up click listeners for buttons.
     */
    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> handleLogin());
        createAccountButton.setOnClickListener(v -> handleCreateAccount());
    }

    /**
     * Handle user login attempt.
     * UPDATED: Now uses ViewModel with callback pattern
     */
    private void handleLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (validateInput(username, password)) {

            // Disable buttons to prevent multiple submissions
            setButtonsEnabled(false);

            // Attempt authentication using ViewModel
            viewModel.authenticateUser(username, password, (userId, authenticatedUsername) -> {
                if (userId > 0) {

                    // Authentication successful
                    saveUserSession(userId, authenticatedUsername);
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                    proceedToPermissions();
                } else {

                    // Authentication failed
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_LONG).show();

                    // Clear password field
                    passwordInput.setText("");
                    passwordInput.requestFocus();
                    setButtonsEnabled(true);
                }
            });
        }
    }

    /**
     * Handle new account creation.
     * UPDATED: Now uses ViewModel with callback pattern
     */
    private void handleCreateAccount() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (validateInput(username, password)) {

            // Disable buttons to prevent multiple submissions
            setButtonsEnabled(false);

            Log.d("LoginActivity", "Attempting to create account for: " + username);

            // Attempt account creation using ViewModel
            viewModel.createUser(username, password, userId -> {
                if (userId > 0) {

                    // Account creation successful
                    saveUserSession(userId, username);
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    proceedToPermissions();
                } else if (userId == -2) {

                    // Username already exists
                    Toast.makeText(this, "Username already exists. Please choose a different username.", Toast.LENGTH_LONG).show();
                    usernameInput.requestFocus();
                    setButtonsEnabled(true);
                } else {

                    // Other error
                    Toast.makeText(this, "Failed to create account. Please try again.", Toast.LENGTH_LONG).show();
                    setButtonsEnabled(true);
                }
            });
        }
    }

    /**
     * Validate user input for username and password.
     */
    private boolean validateInput(String username, String password) {

        // Clear any existing errors
        usernameInput.setError(null);
        passwordInput.setError(null);

        // Validate username
        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("Username is required");
            usernameInput.requestFocus();
            return false;
        }

        if (username.length() < 3) {
            usernameInput.setError("Username must be at least 3 characters");
            usernameInput.requestFocus();
            return false;
        }

        if (username.length() > 20) {
            usernameInput.setError("Username cannot exceed 20 characters");
            usernameInput.requestFocus();
            return false;
        }

        // Check for valid username characters
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            usernameInput.setError("Username can only contain letters, numbers, and underscores");
            usernameInput.requestFocus();
            return false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }

        if (password.length() < 4) {
            passwordInput.setError("Password must be at least 4 characters");
            passwordInput.requestFocus();
            return false;
        }

        if (password.length() > 50) {
            passwordInput.setError("Password cannot exceed 50 characters");
            passwordInput.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Save user session information to SharedPreferences.
     */
    private void saveUserSession(long userId, String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Clear user session information.
     */
    private void clearSession() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USERNAME);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }

    /**
     * Enable or disable login buttons.
     */
    private void setButtonsEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        createAccountButton.setEnabled(enabled);
    }

    /**
     * Navigate to the permissions activity.
     */
    private void proceedToPermissions() {
        Intent intent = new Intent(this, PermissionsActivity.class);
        startActivity(intent);

        // Prevent going back to login screen
        finish();
    }

    /**
     * Get the current user's ID from session.
     */
    public static long getCurrentUserId(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getLong(KEY_USER_ID, -1);
    }

    /**
     * Get the current user's username from session.
     */
    public static String getCurrentUsername(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, "");
    }

    /**
     * Check if user is currently logged in.
     */
    public static boolean isUserLoggedIn(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Log out the current user by clearing session data.
     */
    public static void logoutUser(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}