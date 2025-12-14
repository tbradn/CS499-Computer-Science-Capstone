package com.example.tristenbradneyinventoryapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * InventoryActivity - MVVM Enhanced Version
 *
 * MAJOR CHANGES FOR MVVM ARCHITECTURE:
 * 1. Added ViewModel instead of direct DatabaseHelper access
 * 2. Added LiveData observers for automatic UI updates
 * 3. Validation moved to ViewModel layer
 * 4. Background thread handling managed by Repository
 *
 * This demonstrates separation of concerns and modern Android architecture.
 */
public class InventoryActivity extends AppCompatActivity {

    // UI Components
    private EditText itemNameInput;
    private EditText itemQuantityInput;
    private EditText itemPriceInput;
    private Button addItemButton;
    private Button settingsButton;
    private LinearLayout inventoryContainer;
    private TextView emptyStateText;

    // Data and Helper Classes
    private List<InventoryItemEntity> inventoryItems;
    private NumberFormat currencyFormat;

    // MVVM CHANGE: Replace DatabaseHelper with ViewModel
    // OLD: private DatabaseHelper databaseHelper;
    private InventoryViewModel viewModel;

    private SMSHelper smsHelper;

    // User session information
    private long currentUserId;
    private String currentUsername;

    // Low inventory threshold
    private static final int LOW_INVENTORY_THRESHOLD = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        initializeComponents();
        loadUserSession();
        setupObservers();
        setupClickListeners();
        showWelcomeMessage();
    }

    /**
     * Initialize all UI components and helper classes.
     */
    private void initializeComponents() {

        // Initialize UI components
        itemNameInput = findViewById(R.id.item_name_input);
        itemQuantityInput = findViewById(R.id.item_quantity_input);
        itemPriceInput = findViewById(R.id.item_price_input);
        addItemButton = findViewById(R.id.add_item_button);
        settingsButton = findViewById(R.id.settings_button);
        inventoryContainer = findViewById(R.id.inventory_container);
        emptyStateText = findViewById(R.id.empty_state_text);

        // Initialize data structures
        inventoryItems = new ArrayList<>();
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

        // MVVM CHANGE: Initialize ViewModel instead of DatabaseHelper
        // OLD: databaseHelper = DatabaseHelper.getInstance(this);
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        smsHelper = new SMSHelper(this);
    }

    /**
     * Load current user session information.
     */
    private void loadUserSession() {
        currentUserId = LoginActivity.getCurrentUserId(this);
        currentUsername = LoginActivity.getCurrentUsername(this);

        // Verify user is logged in
        if (currentUserId == -1 || currentUsername.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * NEW METHOD: Setup LiveData observers for automatic UI updates.
     */
    private void setupObservers() {
        // Observe all inventory items - UI updates automatically when data changes
        viewModel.getAllItems().observe(this, items -> {
            if (items != null) {
                inventoryItems.clear();
                inventoryItems.addAll(items);
                updateInventoryDisplay();
            }
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Observe operation success
        viewModel.getOperationSuccess().observe(this, success -> {
            if (success != null && success) {
                clearInputFields();
            }
        });
    }

    /**
     * Show welcome message with shared inventory info.
     */
    private void showWelcomeMessage() {
        String message = "Welcome " + currentUsername + "! You're viewing the shared inventory";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Set up click listeners for all interactive elements.
     */
    private void setupClickListeners() {
        addItemButton.setOnClickListener(v -> addNewItem());
        settingsButton.setOnClickListener(v -> openSettings());
    }

    /**
     * MODIFIED: Add a new inventory item using ViewModel.
     * ViewModel handles validation and database operations on background thread.
     */
    private void addNewItem() {
        String name = itemNameInput.getText().toString().trim();
        String quantityStr = itemQuantityInput.getText().toString().trim();
        String priceStr = itemPriceInput.getText().toString().trim();

        // Basic empty check
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(quantityStr) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);
            double price = Double.parseDouble(priceStr);

            // MVVM CHANGE: Use ViewModel instead of DatabaseHelper
            // OLD: long itemId = databaseHelper.addInventoryItem(newItem, currentUserId);
            viewModel.insert(name, quantity, price, currentUserId);

            // No need to manually update UI - LiveData observer handles it!
            Toast.makeText(this, "Item added to shared inventory!", Toast.LENGTH_SHORT).show();

            // Check for low inventory notification
            if (quantity <= LOW_INVENTORY_THRESHOLD) {
                sendLowInventoryNotification(name, quantity);
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity or price format", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * NEW METHOD: Update inventory display when data changes.
     * Called automatically by LiveData observer.
     */
    private void updateInventoryDisplay() {
        inventoryContainer.removeAllViews();

        for (int i = 0; i < inventoryItems.size(); i++) {
            InventoryItemEntity item = inventoryItems.get(i);
            addItemToView(item, i);
        }

        updateEmptyState();
    }

    /**
     * MODIFIED: Create and add an item view to the inventory container.
     * Changed to use InventoryItemEntity instead of InventoryItem.
     */
    private void addItemToView(InventoryItemEntity item, int position) {

        // Create a horizontal layout for the item row
        LinearLayout itemRow = new LinearLayout(this);
        itemRow.setOrientation(LinearLayout.HORIZONTAL);
        itemRow.setPadding(16, 12, 16, 12);
        itemRow.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 4, 0, 4);
        itemRow.setLayoutParams(rowParams);

        // Item name with last editor info
        TextView nameView = new TextView(this);
        String displayName = item.getItemName();
        if (item.getLastEditorUsername() != null && !item.getLastEditorUsername().isEmpty()
                && !item.getLastEditorUsername().equals("unknown")) {
            displayName += "\n(last edited by " + item.getLastEditorUsername() + ")";
        }
        nameView.setText(displayName);
        nameView.setTextSize(14);
        nameView.setTextColor(0xFF212121);
        nameView.setClickable(true);
        nameView.setOnClickListener(v -> editItemName(item, nameView));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 2f);
        nameView.setLayoutParams(nameParams);

        // Item quantity
        TextView quantityView = new TextView(this);
        quantityView.setText(String.valueOf(item.getQuantity()));
        quantityView.setTextSize(16);
        quantityView.setGravity(android.view.Gravity.CENTER);
        quantityView.setClickable(true);
        quantityView.setOnClickListener(v -> editItemQuantity(item, quantityView));

        // Color coding for low inventory
        if (item.getQuantity() <= LOW_INVENTORY_THRESHOLD) {
            quantityView.setTextColor(0xFFF44336);  // Red
        } else {
            quantityView.setTextColor(0xFF212121);  // Dark gray
        }
        LinearLayout.LayoutParams quantityParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        quantityView.setLayoutParams(quantityParams);

        // Item price
        TextView priceView = new TextView(this);
        priceView.setText(currencyFormat.format(item.getPrice()));
        priceView.setTextSize(16);
        priceView.setTextColor(0xFF212121);
        priceView.setGravity(android.view.Gravity.CENTER);
        priceView.setClickable(true);
        priceView.setOnClickListener(v -> editItemPrice(item, priceView));
        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        priceView.setLayoutParams(priceParams);

        // Delete button
        Button deleteButton = new Button(this);
        deleteButton.setText("Delete");
        deleteButton.setTextSize(12);
        deleteButton.setTextColor(0xFFF44336);
        deleteButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        deleteButton.setLayoutParams(deleteParams);
        deleteButton.setOnClickListener(v -> confirmDeleteItem(position, itemRow, item));

        // Add views to row
        itemRow.addView(nameView);
        itemRow.addView(quantityView);
        itemRow.addView(priceView);
        itemRow.addView(deleteButton);

        // Add row to container
        inventoryContainer.addView(itemRow);
    }

    /**
     * MODIFIED: Edit item name using ViewModel.
     */
    private void editItemName(InventoryItemEntity item, TextView nameView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Item Name");
        builder.setMessage("Editing shared inventory item");

        final EditText input = new EditText(this);
        input.setText(item.getItemName());
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(item.getItemName())) {
                // Update the entity
                item.setItemName(newName);
                item.setLastModifiedBy(currentUserId);

                // MVVM CHANGE: Use ViewModel
                viewModel.update(item, item.getQuantity(), item.getPrice(), currentUserId);

                // UI updates automatically via LiveData!
                Toast.makeText(this, "Item name updated", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * MODIFIED: Edit item quantity using ViewModel.
     */
    private void editItemQuantity(InventoryItemEntity item, TextView quantityView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Quantity");
        builder.setMessage("Editing shared inventory item");

        final EditText input = new EditText(this);
        input.setText(String.valueOf(item.getQuantity()));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String quantityStr = input.getText().toString().trim();
            try {
                int newQuantity = Integer.parseInt(quantityStr);
                if (newQuantity >= 0 && newQuantity != item.getQuantity()) {
                    // MVVM CHANGE: Use ViewModel
                    viewModel.update(item, newQuantity, item.getPrice(), currentUserId);

                    // Check for low inventory
                    if (newQuantity <= LOW_INVENTORY_THRESHOLD) {
                        sendLowInventoryNotification(item.getItemName(), newQuantity);
                    }

                    Toast.makeText(this, "Quantity updated", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * MODIFIED: Edit item price using ViewModel.
     */
    private void editItemPrice(InventoryItemEntity item, TextView priceView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Price");
        builder.setMessage("Editing shared inventory item");

        final EditText input = new EditText(this);
        input.setText(String.valueOf(item.getPrice()));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String priceStr = input.getText().toString().trim();
            try {
                double newPrice = Double.parseDouble(priceStr);
                if (newPrice >= 0 && newPrice != item.getPrice()) {
                    // MVVM CHANGE: Use ViewModel
                    viewModel.update(item, item.getQuantity(), newPrice, currentUserId);
                    Toast.makeText(this, "Price updated", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Confirm deletion of an inventory item.
     */
    private void confirmDeleteItem(int position, LinearLayout itemRow, InventoryItemEntity item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Shared Item");
        builder.setMessage("Are you sure you want to delete '" + item.getItemName() +
                "' from the shared inventory?\n\nThis will affect all users.");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteItem(position, itemRow, item);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * MODIFIED: Delete an inventory item using ViewModel.
     */
    private void deleteItem(int position, LinearLayout itemRow, InventoryItemEntity item) {
        if (position >= 0 && position < inventoryItems.size()) {
            // MVVM CHANGE: Use ViewModel
            viewModel.delete(item);

            // UI updates automatically via LiveData!
            Toast.makeText(this, item.getItemName() + " deleted from shared inventory",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Clear all input fields.
     */
    private void clearInputFields() {
        itemNameInput.setText("");
        itemQuantityInput.setText("");
        itemPriceInput.setText("");
        itemNameInput.requestFocus();
    }

    /**
     * Update the empty state visibility based on inventory count.
     */
    private void updateEmptyState() {
        if (inventoryItems.isEmpty()) {
            inventoryContainer.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateText.setText("No items in shared inventory.\nBe the first to add an item!");
        } else {
            inventoryContainer.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    /**
     * Open settings menu with various options.
     */
    private void openSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings - Shared Inventory");

        String[] options = {
                "Send Test SMS",
                "Check SMS Status",
                "Refresh Inventory",
                "Inventory Statistics",
                "Reset Database (Clear All Data)",
                "Logout",
                "About"
        };

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    smsHelper.sendTestMessage(null);
                    break;
                case 1:
                    String status = smsHelper.getSMSPermissionStatus();
                    Toast.makeText(this, status, Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    // LiveData automatically refreshes - just show message
                    Toast.makeText(this, "Inventory auto-refreshes!", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    showInventoryStatistics();
                    break;
                case 4:
                    confirmDatabaseReset();
                    break;
                case 5:
                    confirmLogout();
                    break;
                case 6:
                    showAboutDialog();
                    break;
            }
        });

        builder.show();
    }

    /**
     * MODIFIED: Confirm database reset using ViewModel.
     */
    private void confirmDatabaseReset() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠️ Reset Database");
        builder.setMessage("This will permanently delete ALL inventory items and user accounts on THIS DEVICE.\n\nThis action cannot be undone.\n\nAre you sure?");

        builder.setPositiveButton("Reset Everything", (dialog, which) -> {
            // MVVM CHANGE: Use ViewModel
            viewModel.deleteAllItems();

            // Clear current session
            LoginActivity.logoutUser(this);

            Toast.makeText(this, "Database reset complete. All data cleared on this device.",
                    Toast.LENGTH_LONG).show();

            // Return to login screen
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton("Cancel", null);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFF44336);
    }

    /**
     * Show inventory statistics.
     */
    private void showInventoryStatistics() {
        String stats = getInventoryStatistics();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Shared Inventory Statistics");
        builder.setMessage(stats);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    /**
     * Confirm logout action.
     */
    private void confirmLogout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");

        builder.setPositiveButton("Logout", (dialog, which) -> {
            LoginActivity.logoutUser(this);
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            finish();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Show about dialog with app information.
     */
    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About Inventory Manager");
        builder.setMessage("Inventory Manager v2.0 - MVVM Enhanced\n\n" +
                "Features:\n" +
                "• MVVM Architecture with Room Database\n" +
                "• Automatic UI updates with LiveData\n" +
                "• Shared inventory across all users\n" +
                "• Add, edit, and delete inventory items\n" +
                "• SMS notifications for low inventory\n" +
                "• User authentication\n" +
                "• Real-time collaboration\n\n" +
                "Current User: " + currentUsername);

        builder.setPositiveButton("OK", null);
        builder.show();
    }

    /**
     * Send low inventory notification via SMS.
     */
    private void sendLowInventoryNotification(String itemName, int quantity) {
        Toast.makeText(this, "Low inventory alert: " + itemName + " (" + quantity + " remaining)",
                Toast.LENGTH_LONG).show();
        smsHelper.sendLowInventoryAlert(itemName, quantity, null);
    }

    /**
     * Get current inventory statistics.
     */
    public String getInventoryStatistics() {
        if (inventoryItems.isEmpty()) {
            return "No items in shared inventory";
        }

        int totalItems = inventoryItems.size();
        int totalQuantity = 0;
        double totalValue = 0.0;
        int lowStockItems = 0;

        for (InventoryItemEntity item : inventoryItems) {
            totalQuantity += item.getQuantity();
            totalValue += item.getTotalValue();
            if (item.isLowStock(LOW_INVENTORY_THRESHOLD)) {
                lowStockItems++;
            }
        }

        return String.format(Locale.US,
                "Shared Inventory Statistics:\n" +
                        "• Total Items: %d\n" +
                        "• Total Quantity: %d\n" +
                        "• Total Value: %s\n" +
                        "• Low Stock Items: %d\n" +
                        "• Current User: %s",
                totalItems, totalQuantity,
                NumberFormat.getCurrencyInstance(Locale.US).format(totalValue),
                lowStockItems, currentUsername);
    }

    /**
     * Handle back button press to prompt for logout.
     */
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Do you want to logout and return to the login screen?");
        builder.setIcon(android.R.drawable.ic_dialog_info);

        builder.setPositiveButton("Logout", (dialog, which) -> {
            LoginActivity.logoutUser(this);
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton("Stay", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // No need to manually refresh - LiveData handles it automatically!
    }
}

/*
 * SUMMARY OF MVVM CHANGES:
 *
 * 1. Replaced DatabaseHelper with InventoryViewModel (line 45)
 * 2. Added setupObservers() method for LiveData (lines 103-126)
 * 3. Changed InventoryItem to InventoryItemEntity throughout
 * 4. All database operations now go through ViewModel
 * 5. UI updates automatically via LiveData observers - no manual refresh needed!
 * 6. FIXED: Changed from Activity to AppCompatActivity for lifecycle support
 *
 * KEY BENEFITS DEMONSTRATED:
 * - Separation of Concerns: Activity only handles UI
 * - Automatic UI Updates: LiveData updates UI when data changes
 * - Background Threading: Repository handles thread management
 * - Testability: ViewModel can be unit tested without Activity
 * - Lifecycle Awareness: LiveData respects Activity lifecycle
 */