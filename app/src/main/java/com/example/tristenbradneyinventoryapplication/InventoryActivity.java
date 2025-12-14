package com.example.tristenbradneyinventoryapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * InventoryActivity is the main screen for managing shared inventory items.
 * Modified to support shared inventory visible across all user accounts.
 */
public class InventoryActivity extends Activity {

    // UI Components.
    private EditText itemNameInput;
    private EditText itemQuantityInput;
    private EditText itemPriceInput;
    private Button addItemButton;
    private Button settingsButton;
    private LinearLayout inventoryContainer;
    private TextView emptyStateText;

    // Data and Helper Classes.
    private List<InventoryItem> inventoryItems;
    private NumberFormat currencyFormat;
    private DatabaseHelper databaseHelper;
    private SMSHelper smsHelper;

    // User session information.
    private long currentUserId;
    private String currentUsername;

    // Low inventory threshold.
    private static final int LOW_INVENTORY_THRESHOLD = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        initializeComponents();
        loadUserSession();
        loadSharedInventoryFromDatabase();
        setupClickListeners();
        updateEmptyState();
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

        // Initialize data structures.
        inventoryItems = new ArrayList<>();
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

        // Initialize helper classes.
        databaseHelper = DatabaseHelper.getInstance(this);
        smsHelper = new SMSHelper(this);
    }

    /**
     * Load current user session information.
     */
    private void loadUserSession() {
        currentUserId = LoginActivity.getCurrentUserId(this);
        currentUsername = LoginActivity.getCurrentUsername(this);

        // Verify user is logged in.
        if (currentUserId == -1 || currentUsername.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Load shared inventory items from database.
     */
    private void loadSharedInventoryFromDatabase() {
        try {

            // Get ALL inventory items.
            List<InventoryItem> dbItems = databaseHelper.getAllInventoryItems();

            inventoryItems.clear();
            inventoryContainer.removeAllViews();

            for (InventoryItem item : dbItems) {
                inventoryItems.add(item);
                addItemToView(item, inventoryItems.size() - 1);
            }

            updateEmptyState();

        } catch (Exception e) {
            Toast.makeText(this, "Error loading inventory: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show welcome message with shared inventory info.
     */
    private void showWelcomeMessage() {
        String message = "Welcome " + currentUsername + "! You're viewing the shared inventory (" +
                inventoryItems.size() + " items)";
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
     * Add a new inventory item to the shared database.
     */
    private void addNewItem() {
        String name = itemNameInput.getText().toString().trim();
        String quantityStr = itemQuantityInput.getText().toString().trim();
        String priceStr = itemPriceInput.getText().toString().trim();

        if (validateItemInput(name, quantityStr, priceStr)) {
            int quantity = Integer.parseInt(quantityStr);
            double price = Double.parseDouble(priceStr);

            InventoryItem newItem = new InventoryItem(name, quantity, price);

            // Add to shared database.
            long itemId = databaseHelper.addInventoryItem(newItem, currentUserId);

            if (itemId > 0) {

                // Add to local list and UI.
                // Set last editor for display.
                newItem.setLastEditedBy(currentUsername);
                inventoryItems.add(newItem);
                addItemToView(newItem, inventoryItems.size() - 1);
                clearInputFields();
                updateEmptyState();

                Toast.makeText(this, "Item added to shared inventory!", Toast.LENGTH_SHORT).show();

                // Check for low inventory notification.
                if (quantity <= LOW_INVENTORY_THRESHOLD) {
                    sendLowInventoryNotification(name, quantity);
                }
            } else if (itemId == -2) {

                // Item name already exists.
                Toast.makeText(this, "An item with this name already exists in the shared inventory", Toast.LENGTH_LONG).show();
                itemNameInput.requestFocus();
            } else {
                Toast.makeText(this, "Failed to add item to shared inventory", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Create and add an item view to the inventory container.
     */
    private void addItemToView(InventoryItem item, int position) {

        // Create a horizontal layout for the item row.
        LinearLayout itemRow = new LinearLayout(this);
        itemRow.setOrientation(LinearLayout.HORIZONTAL);
        itemRow.setPadding(16, 12, 16, 12);

        // White background.
        itemRow.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 4, 0, 4);
        itemRow.setLayoutParams(rowParams);

        // Item name with last editor info.
        TextView nameView = new TextView(this);
        String displayName = item.getName();
        if (item.getLastEditedBy() != null && !item.getLastEditedBy().isEmpty() && !item.getLastEditedBy().equals("unknown")) {
            displayName += "\n(last edited by " + item.getLastEditedBy() + ")";
        }
        nameView.setText(displayName);
        nameView.setTextSize(14);

        // Dark gray.
        nameView.setTextColor(0xFF212121);
        nameView.setClickable(true);
        nameView.setOnClickListener(v -> editItemName(item, nameView));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f);
        nameView.setLayoutParams(nameParams);

        // Item quantity.
        TextView quantityView = new TextView(this);
        quantityView.setText(String.valueOf(item.getQuantity()));
        quantityView.setTextSize(16);
        quantityView.setGravity(android.view.Gravity.CENTER);
        quantityView.setClickable(true);
        quantityView.setOnClickListener(v -> editItemQuantity(item, quantityView));

        // Color coding for low inventory.
        if (item.getQuantity() <= LOW_INVENTORY_THRESHOLD) {

            // Red for low stock.
            quantityView.setTextColor(0xFFF44336);
        } else {

            // Dark gray.
            quantityView.setTextColor(0xFF212121);
        }
        LinearLayout.LayoutParams quantityParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        quantityView.setLayoutParams(quantityParams);

        // Item price.
        TextView priceView = new TextView(this);
        priceView.setText(currencyFormat.format(item.getPrice()));
        priceView.setTextSize(16);

        // Dark gray.
        priceView.setTextColor(0xFF212121);
        priceView.setGravity(android.view.Gravity.CENTER);
        priceView.setClickable(true);
        priceView.setOnClickListener(v -> editItemPrice(item, priceView));
        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        priceView.setLayoutParams(priceParams);

        // Delete button.
        Button deleteButton = new Button(this);
        deleteButton.setText("Delete");
        deleteButton.setTextSize(12);

        // Red text.
        deleteButton.setTextColor(0xFFF44336);
        deleteButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        deleteButton.setLayoutParams(deleteParams);

        deleteButton.setOnClickListener(v -> confirmDeleteItem(position, itemRow, item));

        // Add views to row.
        itemRow.addView(nameView);
        itemRow.addView(quantityView);
        itemRow.addView(priceView);
        itemRow.addView(deleteButton);

        // Add row to container.
        inventoryContainer.addView(itemRow);
    }

    /**
     * Edit item name with a dialog.
     */
    private void editItemName(InventoryItem item, TextView nameView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Item Name");
        builder.setMessage("Editing shared inventory item");

        final EditText input = new EditText(this);
        input.setText(item.getName());
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(item.getName())) {
                String oldName = item.getName();
                item.setName(newName);

                // Update display name with last editor info.
                String displayName = newName;
                if (item.getLastEditedBy() != null && !item.getLastEditedBy().isEmpty() && !item.getLastEditedBy().equals("unknown")) {
                    displayName += "\n(last edited by " + item.getLastEditedBy() + ")";
                }
                nameView.setText(displayName);

                // Update in shared database.
                int result = databaseHelper.updateInventoryItem(oldName, item.getQuantity(), item.getPrice(), currentUserId);
                if (result > 0) {

                    // Update last editor info.
                    item.setLastEditedBy(currentUsername);
                    String updatedDisplayName = newName + "\n(last edited by " + currentUsername + ")";
                    nameView.setText(updatedDisplayName);
                    Toast.makeText(this, "Item name updated in shared inventory", Toast.LENGTH_SHORT).show();
                } else {

                    // Revert on database error.
                    item.setName(oldName);
                    String revertDisplayName = oldName;
                    if (item.getLastEditedBy() != null && !item.getLastEditedBy().isEmpty() && !item.getLastEditedBy().equals("unknown")) {
                        revertDisplayName += "\n(last edited by " + item.getLastEditedBy() + ")";
                    }
                    nameView.setText(revertDisplayName);
                    Toast.makeText(this, "Failed to update item name", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Edit item quantity with a dialog.
     */
    private void editItemQuantity(InventoryItem item, TextView quantityView) {
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
                    int oldQuantity = item.getQuantity();
                    item.setQuantity(newQuantity);
                    quantityView.setText(String.valueOf(newQuantity));

                    // Update color based on new quantity.
                    if (newQuantity <= LOW_INVENTORY_THRESHOLD) {

                        // Red for low stock.
                        quantityView.setTextColor(0xFFF44336);
                        sendLowInventoryNotification(item.getName(), newQuantity);
                    } else {

                        // Dark gray.
                        quantityView.setTextColor(0xFF212121);
                    }

                    // Update in shared database.
                    int result = databaseHelper.updateInventoryItem(item.getName(), newQuantity, item.getPrice(), currentUserId);
                    if (result > 0) {

                        // Update last editor info.
                        item.setLastEditedBy(currentUsername);
                        Toast.makeText(this, "Quantity updated in shared inventory", Toast.LENGTH_SHORT).show();
                    } else {

                        // Revert on database error.
                        item.setQuantity(oldQuantity);
                        quantityView.setText(String.valueOf(oldQuantity));
                        Toast.makeText(this, "Failed to update quantity", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Edit item price with a dialog.
     */
    private void editItemPrice(InventoryItem item, TextView priceView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Price");
        builder.setMessage("Editing shared inventory item");

        final EditText input = new EditText(this);
        input.setText(String.valueOf(item.getPrice()));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String priceStr = input.getText().toString().trim();
            try {
                double newPrice = Double.parseDouble(priceStr);
                if (newPrice >= 0 && newPrice != item.getPrice()) {
                    double oldPrice = item.getPrice();
                    item.setPrice(newPrice);
                    priceView.setText(currencyFormat.format(newPrice));

                    // Update in shared database.
                    int result = databaseHelper.updateInventoryItem(item.getName(), item.getQuantity(), newPrice, currentUserId);
                    if (result > 0) {

                        // Update last editor info.
                        item.setLastEditedBy(currentUsername);
                        Toast.makeText(this, "Price updated in shared inventory", Toast.LENGTH_SHORT).show();
                    } else {

                        // Revert on database error.
                        item.setPrice(oldPrice);
                        priceView.setText(currencyFormat.format(oldPrice));
                        Toast.makeText(this, "Failed to update price", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Confirm deletion of an inventory item from shared inventory.
     */
    private void confirmDeleteItem(int position, LinearLayout itemRow, InventoryItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Shared Item");
        builder.setMessage("Are you sure you want to delete '" + item.getName() + "' from the shared inventory?\n\nThis will affect all users.");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteItem(position, itemRow, item);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Delete an inventory item from shared database and UI.
     */
    private void deleteItem(int position, LinearLayout itemRow, InventoryItem item) {
        if (position >= 0 && position < inventoryItems.size()) {
            String itemName = item.getName();

            // Delete from shared database.
            int result = databaseHelper.deleteInventoryItem(itemName);

            if (result > 0) {

                // Remove from local list and UI.
                inventoryItems.remove(position);
                inventoryContainer.removeView(itemRow);
                updateEmptyState();

                Toast.makeText(this, itemName + " deleted from shared inventory", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to delete item from shared inventory", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Validate input fields for adding new items.
     */
    private boolean validateItemInput(String name, String quantity, String price) {

        // Clear previous errors.
        itemNameInput.setError(null);
        itemQuantityInput.setError(null);
        itemPriceInput.setError(null);

        if (TextUtils.isEmpty(name)) {
            itemNameInput.setError("Item name is required");
            itemNameInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(quantity)) {
            itemQuantityInput.setError("Quantity is required");
            itemQuantityInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(price)) {
            itemPriceInput.setError("Price is required");
            itemPriceInput.requestFocus();
            return false;
        }

        try {
            int qty = Integer.parseInt(quantity);
            if (qty < 0) {
                itemQuantityInput.setError("Quantity must be positive");
                itemQuantityInput.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            itemQuantityInput.setError("Invalid quantity");
            itemQuantityInput.requestFocus();
            return false;
        }

        try {
            double priceValue = Double.parseDouble(price);
            if (priceValue < 0) {
                itemPriceInput.setError("Price must be positive");
                itemPriceInput.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            itemPriceInput.setError("Invalid price");
            itemPriceInput.requestFocus();
            return false;
        }

        return true;
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

                    // Send Test SMS.
                    smsHelper.sendTestMessage(null);
                    break;
                case 1:

                    // Check SMS Status.
                    String status = smsHelper.getSMSPermissionStatus();
                    Toast.makeText(this, status, Toast.LENGTH_LONG).show();
                    break;
                case 2:

                    // Refresh Inventory.
                    refreshInventory();
                    break;
                case 3:

                    // Inventory Statistics.
                    showInventoryStatistics();
                    break;
                case 4:

                    // Reset Database.
                    confirmDatabaseReset();
                    break;
                case 5:

                    // Logout.
                    confirmLogout();
                    break;
                case 6:

                    // About.
                    showAboutDialog();
                    break;
            }
        });

        builder.show();
    }

    /**
     * Confirm database reset action.
     */
    private void confirmDatabaseReset() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠️ Reset Database");
        builder.setMessage("This will permanently delete ALL inventory items and user accounts on THIS DEVICE.\n\nThis action cannot be undone.\n\nAre you sure?");

        builder.setPositiveButton("Reset Everything", (dialog, which) -> {

            // Reset the database.
            //databaseHelper.resetDatabase();
            databaseHelper.clearAllInventory();

            // Clear current session.
            LoginActivity.logoutUser(this);

            // Refresh the UI.
            loadSharedInventoryFromDatabase();

            Toast.makeText(this, "Database reset complete. All data cleared on this device.", Toast.LENGTH_LONG).show();

            // Return to login screen.
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton("Cancel", null);

        // Make dialog unable to cancel by tapping outside for safety.
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Make the reset button red to indicate danger.
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
        builder.setMessage("Inventory Manager v1.0 - Shared Edition\n\n" +
                "Features:\n" +
                "• Shared inventory across all users\n" +
                "• Add, edit, and delete inventory items\n" +
                "• Persistent database storage\n" +
                "• SMS notifications for low inventory\n" +
                "• User authentication\n" +
                "• Real-time collaboration\n\n" +
                "Current User: " + currentUsername + "\n" +
                "Total Users Can Access This Inventory");

        builder.setPositiveButton("OK", null);
        builder.show();
    }

    /**
     * Send low inventory notification via SMS.
     */
    private void sendLowInventoryNotification(String itemName, int quantity) {

        // First show local toast notification.
        Toast.makeText(this, "Low inventory alert: " + itemName + " (" + quantity + " remaining)",
                Toast.LENGTH_LONG).show();

        // Then attempt to send SMS if permission is granted.
        smsHelper.sendLowInventoryAlert(itemName, quantity, null);
    }

    /**
     * Enhanced InventoryItem class with last editor tracking.
     */
    public static class InventoryItem {
        private String name;
        private int quantity;
        private double price;
        private long id;

        // Track who last edited the item.
        private String lastEditedBy;

        public InventoryItem(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
            this.id = -1;
            this.lastEditedBy = "";
        }

        public InventoryItem(long id, String name, int quantity, double price) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
            this.lastEditedBy = "";
        }

        // Getters.
        public long getId() { return id; }
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public String getLastEditedBy() { return lastEditedBy; }

        // Setters.
        public void setId(long id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public void setPrice(double price) { this.price = price; }
        public void setLastEditedBy(String lastEditedBy) { this.lastEditedBy = lastEditedBy; }

        public double getTotalValue() {
            return quantity * price;
        }

        public boolean isLowInventory(int threshold) {
            return quantity <= threshold;
        }

        public String getFormattedPrice() {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
            return currencyFormat.format(price);
        }

        public String getFormattedTotalValue() {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
            return currencyFormat.format(getTotalValue());
        }

        @Override
        public String toString() {
            return String.format("InventoryItem{name='%s', quantity=%d, price=%.2f, lastEditedBy='%s'}",
                    name, quantity, price, lastEditedBy);
        }
    }

    /**
     * Refresh inventory data from database.
     */
    public void refreshInventory() {
        loadSharedInventoryFromDatabase();
        Toast.makeText(this, "Shared inventory refreshed", Toast.LENGTH_SHORT).show();
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

        for (InventoryItem item : inventoryItems) {
            totalQuantity += item.getQuantity();
            totalValue += item.getTotalValue();
            if (item.isLowInventory(LOW_INVENTORY_THRESHOLD)) {
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

            // Logout the user and return to login screen.
            LoginActivity.logoutUser(this);
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            // Return to login screen.
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton("Stay", (dialog, which) -> {

            // Just dismiss the dialog and stay in the app.
            dialog.dismiss();
        });

        // Make dialog non-cancelable by tapping outside.
        builder.setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh inventory when returning to activity to show latest changes.
        refreshInventory();
    }
}