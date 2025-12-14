package com.example.tristenbradneyinventoryapplication;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

/**
 * ViewModel for Inventory operations.
 * Holds UI-related data and business logic.
 * Survives configuration changes (like screen rotations).
 */
public class InventoryViewModel extends AndroidViewModel {

    private InventoryRepository repository;
    private LiveData<List<InventoryItemEntity>> allItems;
    private MutableLiveData<String> errorMessage;
    private MutableLiveData<Boolean> operationSuccess;

    public InventoryViewModel(@NonNull Application application) {
        super(application);
        repository = new InventoryRepository(application);
        allItems = repository.getAllItems();
        errorMessage = new MutableLiveData<>();
        operationSuccess = new MutableLiveData<>();
    }

    // ========== GETTERS FOR LIVEDATA ==========

    public LiveData<List<InventoryItemEntity>> getAllItems() {
        return allItems;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getOperationSuccess() {
        return operationSuccess;
    }

    // ========== INVENTORY OPERATIONS ==========

    /**
     * Insert a new inventory item with validation.
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
            } else {
                errorMessage.postValue("Failed to update item");
                operationSuccess.postValue(false);
            }
        });
    }

    /**
     * Delete an inventory item.
     */
    public void delete(InventoryItemEntity item) {
        repository.delete(item, success -> {
            if (success) {
                operationSuccess.postValue(true);
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
     * Get low stock items.
     */
    public LiveData<List<InventoryItemEntity>> getLowStockItems(int threshold) {
        return repository.getLowStockItems(threshold);
    }

    /**
     * Search items by name.
     */
    public LiveData<List<InventoryItemEntity>> searchItems(String query) {
        return repository.searchItems(query);
    }

    /**
     * Delete all items.
     */
    public void deleteAllItems() {
        repository.deleteAllItems();
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