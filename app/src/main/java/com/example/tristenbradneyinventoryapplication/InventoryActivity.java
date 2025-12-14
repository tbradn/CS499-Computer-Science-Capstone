package com.example.tristenbradneyinventoryapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.tristenbradneyinventoryapplication.utils.InventorySorter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * InventoryActivity - MVVM Enhanced Version with Algorithms and Data Structures
 *
 * ENHANCEMENT ONE (MVVM):
 * 1. Added ViewModel instead of direct DatabaseHelper access
 * 2. Added LiveData observers for automatic UI updates
 * 3. Validation moved to ViewModel layer
 * 4. Background thread handling managed by Repository
 *
 * ENHANCEMENT TWO (Algorithms and Data Structures):
 * 1. Search functionality - Real-time filtering with O(n) linear search
 * 2. Sorting capabilities - Multiple sort modes with O(n log n) TimSort
 * 3. Low-stock priority system - Min-heap priority queue with O(1) access to urgent items
 *
 * This demonstrates separation of concerns and modern Android architecture
 * combined with efficient algorithms and data structures.
 */
public class InventoryActivity extends AppCompatActivity {

    // Original UI Components
    private EditText itemNameInput;
    private EditText itemQuantityInput;
    private EditText itemPriceInput;
    private Button addItemButton;
    private Button settingsButton;
    private LinearLayout inventoryContainer;
    private TextView emptyStateText;

    // ENHANCEMENT TWO: New UI Components for search, sort, and alerts
    private EditText searchEditText;
    private Button sortByNameButton;
    private Button sortByQuantityButton;
    private Button sortByPriceButton;
    private Button toggleSortButton;
    private TextView lowStockAlertText;

    // Data and Helper Classes
    private List<InventoryItemEntity> inventoryItems;
    private NumberFormat currencyFormat;
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

        // ENHANCEMENT TWO: Initial data load
        viewModel.refreshDisplayedItems();
        viewModel.updateLowStockAlerts();
    }

    /**
     * Initialize all UI components and helper classes.
     * ENHANCED: Now includes search, sort, and alert UI components.
     */
    private void initializeComponents() {

        // Initialize original UI components
        itemNameInput = findViewById(R.id.item_name_input);
        itemQuantityInput = findViewById(R.id.item_quantity_input);
        itemPriceInput = findViewById(R.id.item_price_input);
        addItemButton = findViewById(R.id.add_item_button);
        settingsButton = findViewById(R.id.settings_button);
        inventoryContainer = findViewById(R.id.inventory_container);
        emptyStateText = findViewById(R.id.empty_state_text);

        // ENHANCEMENT TWO: Initialize new UI components
        searchEditText = findViewById(R.id.searchEditText);
        sortByNameButton = findViewById(R.id.sortByNameButton);
        sortByQuantityButton = findViewById(R.id.sortByQuantityButton);
        sortByPriceButton = findViewById(R.id.sortByPriceButton);
        toggleSortButton = findViewById(R.id.toggleSortButton);
        lowStockAlertText = findViewById(R.id.lowStockAlertText);

        // Initialize data structures
        inventoryItems = new ArrayList<>();
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

        // Initialize ViewModel
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
     * Setup LiveData observers for automatic UI updates.
     * ENHANCED: Now observes displayedItems (filtered/sorted) instead of allItems.
     */
    private void setupObservers() {
        // ENHANCEMENT TWO: Observe displayed items (with search and sort applied)
        // This replaces the original observer on allItems
        viewModel.getDisplayedItems().observe(this, items -> {
            if (items != null) {
                inventoryItems.clear();
                inventoryItems.addAll(items);
                updateInventoryDisplay();
            }
        });

        // ENHANCEMENT TWO: Observe low-stock items for priority alerts
        viewModel.getLowStockItems().observe(this, lowStockItems -> {
            updateLowStockAlertDisplay(lowStockItems);
        });

        // ENHANCEMENT TWO: Observe low-stock status for detailed statistics
        viewModel.getLowStockStatus().observe(this, status -> {
            if (status != null && status.totalCount > 0) {
                String alertText = String.format(
                        "Low Stock Alert - Critical: %d | Warning: %d | Low: %d",
                        status.criticalCount,
                        status.warningCount,
                        status.lowStockCount
                );
                lowStockAlertText.setText(alertText);
                lowStockAlertText.setVisibility(View.VISIBLE);

                // Set background color based on highest priority level
                if (status.criticalCount > 0) {
                    lowStockAlertText.setBackgroundColor(Color.parseColor("#FFEBEE")); // Red
                } else if (status.warningCount > 0) {
                    lowStockAlertText.setBackgroundColor(Color.parseColor("#FFF3E0")); // Orange
                } else {
                    lowStockAlertText.setBackgroundColor(Color.parseColor("#FFFDE7")); // Yellow
                }
            } else {
                lowStockAlertText.setVisibility(View.GONE);
            }
        });

        // Original observers
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

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
     * ENHANCED: Now includes search, sort, and toggle button listeners.
     */
    private void setupClickListeners() {
        // Original listeners
        addItemButton.setOnClickListener(v -> addNewItem());
        settingsButton.setOnClickListener(v -> openSettings());

        // ENHANCEMENT TWO: Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Trigger search on each character change
                viewModel.searchInventoryItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // ENHANCEMENT TWO: Sort button listeners
        sortByNameButton.setOnClickListener(v -> {
            viewModel.setSortMode(InventorySorter.SortMode.NAME_ASC);
            highlightActiveButton(sortByNameButton);
            Toast.makeText(this, "Sorting by Name", Toast.LENGTH_SHORT).show();
        });

        sortByQuantityButton.setOnClickListener(v -> {
            viewModel.setSortMode(InventorySorter.SortMode.QUANTITY_ASC);
            highlightActiveButton(sortByQuantityButton);
            Toast.makeText(this, "Sorting by Quantity", Toast.LENGTH_SHORT).show();
        });

        sortByPriceButton.setOnClickListener(v -> {
            viewModel.setSortMode(InventorySorter.SortMode.PRICE_ASC);
            highlightActiveButton(sortByPriceButton);
            Toast.makeText(this, "Sorting by Price", Toast.LENGTH_SHORT).show();
        });

        // ENHANCEMENT TWO: Toggle sort order (ascending <-> descending)
        toggleSortButton.setOnClickListener(v -> {
            viewModel.toggleSortOrder();
            // Update icon to show current direction
            InventorySorter.SortMode currentMode = viewModel.getCurrentSortMode().getValue();
            if (currentMode != null) {
                String direction = currentMode.name().endsWith("ASC") ? "↑" : "↓";
                toggleSortButton.setText(direction);
            }
        });

        // ENHANCEMENT TWO: Make low-stock alert clickable to show details
        lowStockAlertText.setOnClickListener(v -> showLowStockDetailsDialog());
    }

    // ========== ENHANCEMENT TWO: NEW HELPER METHODS ==========

    /**
     * Highlights the active sort button and resets others.
     */
    private void highlightActiveButton(Button activeButton) {
        // Reset all buttons to default state
        sortByNameButton.setBackgroundColor(Color.TRANSPARENT);
        sortByQuantityButton.setBackgroundColor(Color.TRANSPARENT);
        sortByPriceButton.setBackgroundColor(Color.TRANSPARENT);

        // Highlight the active button
        activeButton.setBackgroundColor(Color.parseColor("#E3F2FD")); // Light blue
    }

    /**
     * Updates the low-stock alert display with most urgent item.
     */
    private void updateLowStockAlertDisplay(List<InventoryItemEntity> lowStockItems) {
        if (lowStockItems == null || lowStockItems.isEmpty()) {
            lowStockAlertText.setVisibility(View.GONE);
            return;
        }

        // Get the most urgent item (O(1) operation!)
        InventoryItemEntity mostUrgent = viewModel.getMostUrgentItem();
        if (mostUrgent != null) {
            String message = String.format(
                    "Most Urgent: %s (Qty: %d / Threshold: %d)",
                    mostUrgent.getItemName(),
                    mostUrgent.getQuantity(),
                    10 // Using default threshold
            );
            lowStockAlertText.setText(message);
            lowStockAlertText.setVisibility(View.VISIBLE);

            // Set text color based on urgency
            double ratio = (double) mostUrgent.getQuantity() / 10;
            if (ratio < 0.25) {
                lowStockAlertText.setTextColor(Color.parseColor("#D32F2F")); // Critical - Red
            } else if (ratio < 0.75) {
                lowStockAlertText.setTextColor(Color.parseColor("#F57C00")); // Warning - Orange
            } else {
                lowStockAlertText.setTextColor(Color.parseColor("#F9A825")); // Low Stock - Yellow
            }
        }
    }

    /**
     * Shows a dialog with all low-stock items categorized by urgency.
     */
    private void showLowStockDetailsDialog() {
        List<InventoryItemEntity> lowStockItems = viewModel.getLowStockItems().getValue();
        if (lowStockItems == null || lowStockItems.isEmpty()) {
            Toast.makeText(this, "No low-stock items", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build message with all low-stock items (already sorted by priority!)
        StringBuilder message = new StringBuilder("Low Stock Items (by urgency):\n\n");

        List<InventoryItemEntity> criticalItems = viewModel.getCriticalItems();
        if (!criticalItems.isEmpty()) {
            message.append("CRITICAL (< 25%):\n");
            for (InventoryItemEntity item : criticalItems) {
                message.append(String.format("  • %s: Qty %d (Threshold: %d)\n",
                        item.getItemName(), item.getQuantity(), 10));
            }
            message.append("\n");
        }

        // Show all low-stock items
        message.append("ALL LOW-STOCK ITEMS:\n");
        for (InventoryItemEntity item : lowStockItems) {
            double ratio = (double) item.getQuantity() / 10;
            String urgencyIcon = ratio < 0.25 ? "🔴" : ratio < 0.75 ? "🟠" : "🟡";
            message.append(String.format("  %s %s: Qty %d (Threshold: %d)\n",
                    urgencyIcon, item.getItemName(), item.getQuantity(), 10));
        }

        // Show dialog
        new AlertDialog.Builder(this)
                .setTitle("Low Stock Report")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("Send SMS Alerts", (dialog, which) -> {
                    // Send SMS for all low-stock items
                    for (InventoryItemEntity item : lowStockItems) {
                        sendLowInventoryNotification(item.getItemName(), item.getQuantity());
                    }
                })
                .show();
    }

    // ========== ORIGINAL METHODS (UNCHANGED) ==========

    /**
     * Add a new inventory item using ViewModel.
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

            viewModel.insert(name, quantity, price, currentUserId);
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
     * Update inventory display when data changes.
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
     * Create and add an item view to the inventory container.
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
     * Edit item name using ViewModel.
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
                item.setItemName(newName);
                item.setLastModifiedBy(currentUserId);
                viewModel.update(item, item.getQuantity(), item.getPrice(), currentUserId);
                Toast.makeText(this, "Item name updated", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Edit item quantity using ViewModel.
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
     * Edit item price using ViewModel.
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
     * Delete an inventory item using ViewModel.
     */
    private void deleteItem(int position, LinearLayout itemRow, InventoryItemEntity item) {
        if (position >= 0 && position < inventoryItems.size()) {
            viewModel.delete(item);
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

            // ENHANCEMENT TWO: Show different message if search is active
            String searchQuery = viewModel.getCurrentSearchQuery().getValue();
            if (searchQuery != null && !searchQuery.isEmpty()) {
                emptyStateText.setText("No items found matching '" + searchQuery + "'");
            } else {
                emptyStateText.setText("No items in shared inventory.\nBe the first to add an item!");
            }
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

                // ENHANCEMENT TWO: New option
                "Clear Search & Sort",
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
                    viewModel.refreshDisplayedItems();
                    viewModel.updateLowStockAlerts();
                    Toast.makeText(this, "Inventory refreshed!", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    showInventoryStatistics();
                    break;

                // ENHANCEMENT TWO: Clear search and reset sort
                case 4:
                    searchEditText.setText("");
                    viewModel.clearSearch();
                    viewModel.setSortMode(InventorySorter.SortMode.NAME_ASC);
                    highlightActiveButton(sortByNameButton);
                    Toast.makeText(this, "Search cleared, sorting by name", Toast.LENGTH_SHORT).show();
                    break;
                case 5:
                    confirmDatabaseReset();
                    break;
                case 6:
                    confirmLogout();
                    break;
                case 7:
                    showAboutDialog();
                    break;
            }
        });

        builder.show();
    }

    /**
     * Confirm database reset using ViewModel.
     */
    private void confirmDatabaseReset() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Database");
        builder.setMessage("This will permanently delete ALL inventory items and user accounts on THIS DEVICE.\n\nThis action cannot be undone.\n\nAre you sure?");

        builder.setPositiveButton("Reset Everything", (dialog, which) -> {
            viewModel.deleteAllItems();
            LoginActivity.logoutUser(this);
            Toast.makeText(this, "Database reset complete. All data cleared on this device.",
                    Toast.LENGTH_LONG).show();

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
     * ENHANCED: Now uses displayed items (filtered/sorted) for statistics.
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
     * ENHANCED: Updated version info.
     */
    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About Inventory Manager");
        builder.setMessage("Inventory Manager v3.0 - Enhanced with Algorithms\n\n" +
                "Features:\n" +
                "• MVVM Architecture with Room Database\n" +
                "• Automatic UI updates with LiveData\n" +
                "• Real-time search (O(n) linear search)\n" +
                "• Multiple sort options (O(n log n) TimSort)\n" +
                "• Low-stock priority queue (O(1) access)\n" +
                "• Shared inventory across all users\n" +
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
     * ENHANCED: Now uses displayed items (respects search/filter).
     */
    public String getInventoryStatistics() {
        if (inventoryItems.isEmpty()) {
            String searchQuery = viewModel.getCurrentSearchQuery().getValue();
            if (searchQuery != null && !searchQuery.isEmpty()) {
                return "No items found matching '" + searchQuery + "'";
            }
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

        // ENHANCEMENT TWO: Add search/sort info to statistics
        String searchInfo = "";
        String searchQuery = viewModel.getCurrentSearchQuery().getValue();
        if (searchQuery != null && !searchQuery.isEmpty()) {
            searchInfo = "• Filtered by: '" + searchQuery + "'\n";
        }

        InventorySorter.SortMode sortMode = viewModel.getCurrentSortMode().getValue();
        String sortInfo = "";
        if (sortMode != null) {
            sortInfo = "• Sorted by: " + sortMode.name().replace("_", " ") + "\n";
        }

        return String.format(Locale.US,
                "Shared Inventory Statistics:\n" +
                        searchInfo +
                        sortInfo +
                        "• Total Items Displayed: %d\n" +
                        "• Total Quantity: %d\n" +
                        "• Total Value: %s\n" +
                        "• Low Stock Items: %d\n" +
                        "• Current User: %s",
                totalItems, totalQuantity,
                NumberFormat.getCurrencyInstance(Locale.US).format(totalValue),
                lowStockItems, currentUsername);
    }

    @SuppressWarnings("deprecation")
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

    /**
     * Show logout confirmation dialog.
     */
    private void showLogoutDialog() {
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
        // ENHANCEMENT TWO: Refresh displayed items and alerts
        viewModel.refreshDisplayedItems();
        viewModel.updateLowStockAlerts();
    }
}