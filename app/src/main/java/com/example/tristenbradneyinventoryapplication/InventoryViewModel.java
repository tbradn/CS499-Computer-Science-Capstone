package com.example.tristenbradneyinventoryapplication;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.tristenbradneyinventoryapplication.utils.InventorySearchManager;
import com.example.tristenbradneyinventoryapplication.utils.InventorySorter;
import com.example.tristenbradneyinventoryapplication.utils.InventorySorter.SortMode;
import com.example.tristenbradneyinventoryapplication.utils.LowStockManager;
import com.example.tristenbradneyinventoryapplication.InventoryRepository.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for Inventory operations - ENHANCED WITH ALGORITHMS AND DATA STRUCTURES
 *
 * ENHANCEMENT TWO ADDITIONS:
 * - Search functionality using linear search algorithm O(n)
 * - Sorting capabilities using TimSort algorithm O(n log n)
 * - Low-stock priority system using min-heap priority queue O(1) access
 *
 * Original features:
 * - Holds UI-related data and business logic
 * - Survives configuration changes (like screen rotations)
 * - Uses MVVM architecture with Room database
 */

private final MutableLiveData<List<AuditLogEntity>> auditHistory = new MutableLiveData<>();
private final MutableLiveData<Integer> auditLogCount = new MutableLiveData<>();

/**
 * Get audit history for a specific item
 *
 * @param itemId The ID of the item
 * @return LiveData list of audit log entries
 */
public LiveData<List<AuditLogEntity>> getAuditHistoryForItem(long itemId) {
    return repository.getAuditHistoryForItem(itemId);
}

/**
 * Get audit history for current user
 *
 * @param userId The ID of the user
 * @return LiveData list of audit log entries
 */
public LiveData<List<AuditLogEntity>> getAuditHistoryForUser(long userId) {
    return repository.getAuditHistoryForUser(userId);
}

/**
 * Get recent audit logs
 *
 * @return LiveData list of recent audit log entries
 */
public LiveData<List<AuditLogEntity>> getRecentAuditLogs() {
    return repository.getRecentAuditLogs();
}

/**
 * Get total count of audit log entries
 */
public void loadAuditLogCount() {
    repository.getAuditLogCount(new RepositoryCallback<Integer>() {
        @Override
        public void onSuccess(Integer result) {
            auditLogCount.postValue(result);
        }

        @Override
        public void onError(String error) {
            errorMessage.postValue("Failed to load audit count: " + error);
        }
    });
}

/**
 * Get audit log count LiveData
 */
public LiveData<Integer> getAuditLogCount() {
    return auditLogCount;
}

// ========== UPDATED INSERT/UPDATE/DELETE WITH CALLBACKS ==========

/**
 * Insert item - UPDATED to use callbacks for better error handling
 */
public void insert(String itemName, int quantity, double price, long userId) {
    // Validate input
    String validationError = validateInventoryItem(itemName, quantity, price);
    if (validationError != null) {
        errorMessage.setValue(validationError);
        return;
    }

    InventoryItemEntity item = new InventoryItemEntity(itemName, quantity, price, userId);

    repository.insert(item, userId, new RepositoryCallback<Long>() {
        @Override
        public void onSuccess(Long itemId) {
            operationSuccess.postValue(true);
        }

        @Override
        public void onError(String error) {
            errorMessage.postValue(error);
            operationSuccess.postValue(false);
        }
    });
}

/**
 * Update item - UPDATED to use callbacks
 */
public void update(InventoryItemEntity item, int newQuantity, double newPrice, long userId) {
    // Validate input
    String validationError = validateQuantityAndPrice(newQuantity, newPrice);
    if (validationError != null) {
        errorMessage.setValue(validationError);
        return;
    }

    repository.update(item, newQuantity, newPrice, userId, new RepositoryCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
            operationSuccess.postValue(true);
            refreshDisplayedItems();
            updateLowStockAlerts();
        }

        @Override
        public void onError(String error) {
            errorMessage.postValue(error);
            operationSuccess.postValue(false);
        }
    });
}

/**
 * Update item name - NEW method with audit logging
 */
public void updateName(InventoryItemEntity item, String newName, long userId) {
    // Validate name
    if (!InventoryItemEntity.isValidName(newName)) {
        errorMessage.setValue("Invalid item name");
        return;
    }

    repository.updateName(item, newName, userId, new RepositoryCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
            operationSuccess.postValue(true);
            refreshDisplayedItems();
        }

        @Override
        public void onError(String error) {
            errorMessage.postValue(error);
            operationSuccess.postValue(false);
        }
    });
}

/**
 * Delete item - UPDATED to use callbacks
 */
public void delete(InventoryItemEntity item, long userId) {
    repository.delete(item, userId, new RepositoryCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
            operationSuccess.postValue(true);
            refreshDisplayedItems();
            updateLowStockAlerts();
        }

        @Override
        public void onError(String error) {
            errorMessage.postValue(error);
            operationSuccess.postValue(false);
        }
    });
}

/**
 * Delete all items - UPDATED to use callbacks
 */
public void deleteAllItems(long userId) {
    repository.deleteAllItems(userId, new RepositoryCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
            operationSuccess.postValue(true);
            refreshDisplayedItems();
            updateLowStockAlerts();
        }

        @Override
        public void onError(String error) {
            errorMessage.postValue(error);
            operationSuccess.postValue(false);
        }
    });
}

public class InventoryViewModel extends AndroidViewModel {

    // Repository for data access
    private InventoryRepository repository;
    private LiveData<List<InventoryItemEntity>> allItems;

    // Original LiveData
    private MutableLiveData<String> errorMessage;
    private MutableLiveData<Boolean> operationSuccess;

    // ========== ENHANCEMENT TWO: NEW ALGORITHM COMPONENTS ==========

    // Algorithm and data structure components
    private final InventorySearchManager searchManager;
    private final InventorySorter sorter;
    private final LowStockManager lowStockManager;

    // New LiveData for search/sort/low-stock features
    private final MutableLiveData<List<InventoryItemEntity>> displayedItems;
    private final MutableLiveData<List<InventoryItemEntity>> lowStockItems;
    private final MutableLiveData<String> currentSearchQuery;
    private final MutableLiveData<SortMode> currentSortMode;
    private final MutableLiveData<LowStockManager.LowStockStatus> lowStockStatus;

    public InventoryViewModel(@NonNull Application application) {
        super(application);
        repository = new InventoryRepository(application);
        allItems = repository.getAllItems();
        errorMessage = new MutableLiveData<>();
        operationSuccess = new MutableLiveData<>();

        // Initialize algorithm components
        searchManager = new InventorySearchManager();
        sorter = new InventorySorter();
        lowStockManager = new LowStockManager();

        // Initialize new LiveData
        displayedItems = new MutableLiveData<>(new ArrayList<>());
        lowStockItems = new MutableLiveData<>(new ArrayList<>());
        currentSearchQuery = new MutableLiveData<>("");
        currentSortMode = new MutableLiveData<>(SortMode.NAME_ASC);
        lowStockStatus = new MutableLiveData<>();
    }

    // ========== ORIGINAL GETTERS FOR LIVEDATA ==========

    public LiveData<List<InventoryItemEntity>> getAllItems() {
        return allItems;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getOperationSuccess() {
        return operationSuccess;
    }

    // ========== ENHANCEMENT TWO: NEW GETTERS FOR LIVEDATA ==========

    /**
     * Gets the currently displayed items (after search and sort applied).
     * Use this instead of getAllItems() to display filtered/sorted data.
     */
    public LiveData<List<InventoryItemEntity>> getDisplayedItems() {
        return displayedItems;
    }

    /**
     * Gets low-stock items in priority order (most urgent first).
     */
    public LiveData<List<InventoryItemEntity>> getLowStockItems() {
        return lowStockItems;
    }

    /**
     * Gets the current search query.
     */
    public LiveData<String> getCurrentSearchQuery() {
        return currentSearchQuery;
    }

    /**
     * Gets the current sort mode.
     */
    public LiveData<SortMode> getCurrentSortMode() {
        return currentSortMode;
    }

    /**
     * Gets low-stock status summary with categorized counts.
     */
    public LiveData<LowStockManager.LowStockStatus> getLowStockStatus() {
        return lowStockStatus;
    }

    // ========== ORIGINAL INVENTORY OPERATIONS (ENHANCED) ==========

    /**
     * Insert a new inventory item with validation.
     * ENHANCED: Now calls refresh methods after successful insert.
     */
    public void insert(String itemName, int quantity, double price, long userId) {
        // Validate input
        ValidationResult validation = validateInventoryItem(itemName, quantity, price);

        if (!validation.isValid()) {
            errorMessage.postValue(validation.getErrorMessage());
            operationSuccess.postValue(false);
            return;
        }

        // Create entity and insert
        InventoryItemEntity item = new InventoryItemEntity(
                itemName.trim(),
                quantity,
                price,
                userId,
                userId
        );

        repository.insert(item, itemId -> {
            if (itemId > 0) {
                operationSuccess.postValue(true);
                // ENHANCEMENT TWO: Refresh display and update alerts
                refreshDisplayedItems();
                updateLowStockAlerts();
            } else if (itemId == -2) {
                errorMessage.postValue("Item name already exists");
                operationSuccess.postValue(false);
            } else {
                errorMessage.postValue("Failed to add item");
                operationSuccess.postValue(false);
            }
        });
    }

    /**
     * Update an existing inventory item.
     * ENHANCED: Now calls refresh methods after successful update.
     */
    public void update(InventoryItemEntity item, int newQuantity, double newPrice, long userId) {
        // Validate input
        ValidationResult validation = validateQuantityAndPrice(newQuantity, newPrice);

        if (!validation.isValid()) {
            errorMessage.postValue(validation.getErrorMessage());
            operationSuccess.postValue(false);
            return;
        }

        // Update item properties
        item.setQuantity(newQuantity);
        item.setPrice(newPrice);
        item.setLastModifiedBy(userId);

        repository.update(item, success -> {
            if (success) {
                operationSuccess.postValue(true);
                // ENHANCEMENT TWO: Refresh display and update alerts
                refreshDisplayedItems();
                updateLowStockAlerts();
            } else {
                errorMessage.postValue("Failed to update item");
                operationSuccess.postValue(false);
            }
        });
    }

    /**
     * Delete an inventory item.
     * ENHANCED: Now calls refresh methods after successful delete.
     */
    public void delete(InventoryItemEntity item) {
        repository.delete(item, success -> {
            if (success) {
                operationSuccess.postValue(true);
                // ENHANCEMENT TWO: Refresh display and update alerts
                refreshDisplayedItems();
                updateLowStockAlerts();
            } else {
                errorMessage.postValue("Failed to delete item");
                operationSuccess.postValue(false);
            }
        });
    }

    /**
     * Get item by name.
     */
    public void getItemByName(String itemName, InventoryRepository.OnItemFetchListener listener) {
        repository.getItemByName(itemName, listener);
    }

    /**
     * Get low stock items (original method - still available).
     */
    public LiveData<List<InventoryItemEntity>> getLowStockItems(int threshold) {
        return repository.getLowStockItems(threshold);
    }

    /**
     * Search items by name (original method - still available).
     */
    public LiveData<List<InventoryItemEntity>> searchItems(String query) {
        return repository.searchItems(query);
    }

    /**
     * Delete all items.
     * ENHANCED: Now calls refresh methods after deletion.
     */
    public void deleteAllItems() {
        repository.deleteAllItems();
        refreshDisplayedItems();
        updateLowStockAlerts();
    }

    // ========== ENHANCEMENT TWO: SEARCH FUNCTIONALITY ==========

    /**
     * Performs search and updates displayed items.
     *
     * Algorithm: Linear search with substring matching
     * Time Complexity: O(n) where n is the number of items
     * Space Complexity: O(m) where m is the number of matching items
     *
     * @param query Search term to filter items
     */
    public void searchInventoryItems(String query) {
        currentSearchQuery.setValue(query);
        applySearchAndSort();
    }

    /**
     * Clears the current search and shows all items.
     */
    public void clearSearch() {
        currentSearchQuery.setValue("");
        applySearchAndSort();
    }

    // ========== ENHANCEMENT TWO: SORTING FUNCTIONALITY ==========

    /**
     * Changes the sort mode and updates displayed items.
     *
     * Algorithm: TimSort (via Java Collections.sort)
     * Time Complexity: O(n log n) average and worst case
     *
     * @param sortMode The new sort mode to apply
     */
    public void setSortMode(SortMode sortMode) {
        currentSortMode.setValue(sortMode);
        applySearchAndSort();
    }

    /**
     * Toggles between ascending and descending for current sort field.
     */
    public void toggleSortOrder() {
        SortMode current = currentSortMode.getValue();
        if (current == null) return;

        SortMode newMode;
        switch (current) {
            case NAME_ASC:
                newMode = SortMode.NAME_DESC;
                break;
            case NAME_DESC:
                newMode = SortMode.NAME_ASC;
                break;
            case QUANTITY_ASC:
                newMode = SortMode.QUANTITY_DESC;
                break;
            case QUANTITY_DESC:
                newMode = SortMode.QUANTITY_ASC;
                break;
            case PRICE_ASC:
                newMode = SortMode.PRICE_DESC;
                break;
            case PRICE_DESC:
                newMode = SortMode.PRICE_ASC;
                break;
            default:
                newMode = SortMode.NAME_ASC;
        }

        setSortMode(newMode);
    }

    // ========== ENHANCEMENT TWO: COMBINED SEARCH AND SORT ==========

    /**
     * Applies current search query and sort mode to the inventory list.
     *
     * Algorithm Flow:
     * 1. Get all items from repository
     * 2. Apply search filter (O(n))
     * 3. Apply sort (O(m log m) where m is search result count)
     * 4. Update displayed items LiveData
     *
     * Overall Time Complexity: O(n + m log m)
     */
    private void applySearchAndSort() {
        List<InventoryItemEntity> items = allItems.getValue();
        if (items == null) {
            items = new ArrayList<>();
        }

        // Step 1: Apply search filter
        String query = currentSearchQuery.getValue();
        List<InventoryItemEntity> searchResults = searchManager.searchByName(items, query);

        // Step 2: Apply sort
        SortMode sortMode = currentSortMode.getValue();
        if (sortMode == null) {
            sortMode = SortMode.NAME_ASC;
        }
        List<InventoryItemEntity> sortedResults = sorter.sort(searchResults, sortMode);

        // Step 3: Update displayed items
        displayedItems.setValue(sortedResults);
    }

    /**
     * Manually triggers refresh of displayed items.
     * Useful when data changes externally.
     */
    public void refreshDisplayedItems() {
        applySearchAndSort();
    }

    // ========== ENHANCEMENT TWO: LOW-STOCK PRIORITY MANAGEMENT ==========

    /**
     * Updates low-stock alerts and priority queue.
     *
     * Data Structure: Min-Heap (Priority Queue)
     * Time Complexity: O(n + m log m) where:
     *   - n is total inventory size (scanning)
     *   - m is number of low-stock items (building heap)
     */
    public void updateLowStockAlerts() {
        List<InventoryItemEntity> items = allItems.getValue();
        if (items == null || items.isEmpty()) {
            lowStockItems.setValue(new ArrayList<>());
            lowStockStatus.setValue(new LowStockManager.LowStockStatus(0, 0, 0));
            return;
        }

        // Update priority queue with all items
        lowStockManager.updateLowStockItems(items);

        // Get prioritized low-stock items (sorted by urgency)
        List<InventoryItemEntity> prioritizedItems = lowStockManager.getLowStockItemsSorted();
        lowStockItems.setValue(prioritizedItems);

        // Update status with categorized counts
        LowStockManager.LowStockStatus status = lowStockManager.getStatus();
        lowStockStatus.setValue(status);
    }

    /**
     * Gets the single most urgent low-stock item.
     *
     * Time Complexity: O(1) - constant time access to heap root
     *
     * @return The most urgent item, or null if no low-stock items
     */
    public InventoryItemEntity getMostUrgentItem() {
        return lowStockManager.getMostUrgentItem();
    }

    /**
     * Gets count of items flagged as low-stock.
     *
     * @return Number of items below or at threshold
     */
    public int getLowStockCount() {
        return lowStockManager.getLowStockCount();
    }

    /**
     * Gets critically low items (quantity < 25% of threshold).
     *
     * @return List of critical items needing immediate attention
     */
    public List<InventoryItemEntity> getCriticalItems() {
        return lowStockManager.getCriticalItems();
    }

    // ========== USER OPERATIONS ==========

    /**
     * Authenticate user.
     */
    public void authenticateUser(String username, String password,
                                 InventoryRepository.OnAuthCompleteListener listener) {
        repository.authenticateUser(username, password, listener);
    }

    /**
     * Create new user account.
     */
    public void createUser(String username, String password,
                           InventoryRepository.OnUserCreateListener listener) {
        repository.createUser(username, password, listener);
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validate inventory item data.
     * This enforces business rules at the ViewModel layer.
     */
    private ValidationResult validateInventoryItem(String itemName, int quantity, double price) {
        // Validate item name
        if (TextUtils.isEmpty(itemName) || itemName.trim().isEmpty()) {
            return new ValidationResult(false, "Item name cannot be empty");
        }

        if (itemName.length() > 100) {
            return new ValidationResult(false, "Item name cannot exceed 100 characters");
        }

        // Check for potentially dangerous characters
        if (itemName.contains("'") || itemName.contains("\"") ||
                itemName.contains(";") || itemName.contains("--")) {
            return new ValidationResult(false, "Item name contains invalid characters");
        }

        // Validate quantity and price
        return validateQuantityAndPrice(quantity, price);
    }

    /**
     * Validate quantity and price.
     */
    private ValidationResult validateQuantityAndPrice(int quantity, double price) {
        if (quantity < 0) {
            return new ValidationResult(false, "Quantity cannot be negative");
        }

        if (quantity > 1000000) {
            return new ValidationResult(false, "Quantity exceeds maximum limit");
        }

        if (price < 0) {
            return new ValidationResult(false, "Price cannot be negative");
        }

        if (price > 1000000) {
            return new ValidationResult(false, "Price exceeds maximum limit");
        }

        return new ValidationResult(true, null);
    }

    // ========== VALIDATION RESULT CLASS ==========

    /**
     * Inner class to hold validation results.
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}