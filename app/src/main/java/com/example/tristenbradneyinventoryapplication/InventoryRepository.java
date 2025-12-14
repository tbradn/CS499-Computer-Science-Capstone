package com.example.tristenbradneyinventoryapplication;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository class for managing data operations.
 * This class abstracts the data layer from the ViewModel.
 * Handles all database operations on background threads.
 */
public class InventoryRepository {

    private static final String TAG = "InventoryRepository";
    private InventoryDao inventoryDao;
    private UserDao userDao;
    private LiveData<List<InventoryItemEntity>> allItems;

    // Executor for background operations
    private ExecutorService executorService;

    /**
     * Constructor initializes DAOs and LiveData.
     */
    public InventoryRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        inventoryDao = database.inventoryDao();
        userDao = database.userDao();
        allItems = inventoryDao.getAllItems();
        executorService = Executors.newFixedThreadPool(2);
    }

    // ========== INVENTORY OPERATIONS ==========

    /**
     * Get all inventory items as LiveData.
     * LiveData automatically updates the UI when data changes.
     */
    public LiveData<List<InventoryItemEntity>> getAllItems() {
        return allItems;
    }

    /**
     * Insert a new inventory item.
     * Executes on background thread.
     */
    public void insert(InventoryItemEntity item, OnInsertCompleteListener listener) {
        executorService.execute(() -> {
            try {
                long itemId = inventoryDao.insertItem(item);
                if (listener != null) {
                    listener.onInsertComplete(itemId);
                }
                Log.d(TAG, "Item inserted with ID: " + itemId);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting item: " + e.getMessage());
                if (listener != null) {
                    listener.onInsertComplete(-1);
                }
            }
        });
    }

    /**
     * Update an existing inventory item.
     */
    public void update(InventoryItemEntity item, OnUpdateCompleteListener listener) {
        executorService.execute(() -> {
            try {
                int rowsAffected = inventoryDao.updateItem(item);
                if (listener != null) {
                    listener.onUpdateComplete(rowsAffected > 0);
                }
                Log.d(TAG, "Item updated: " + rowsAffected + " rows affected");
            } catch (Exception e) {
                Log.e(TAG, "Error updating item: " + e.getMessage());
                if (listener != null) {
                    listener.onUpdateComplete(false);
                }
            }
        });
    }

    /**
     * Delete an inventory item.
     */
    public void delete(InventoryItemEntity item, OnDeleteCompleteListener listener) {
        executorService.execute(() -> {
            try {
                int rowsAffected = inventoryDao.deleteItem(item);
                if (listener != null) {
                    listener.onDeleteComplete(rowsAffected > 0);
                }
                Log.d(TAG, "Item deleted: " + rowsAffected + " rows affected");
            } catch (Exception e) {
                Log.e(TAG, "Error deleting item: " + e.getMessage());
                if (listener != null) {
                    listener.onDeleteComplete(false);
                }
            }
        });
    }

    /**
     * Get item by name.
     */
    public void getItemByName(String itemName, OnItemFetchListener listener) {
        executorService.execute(() -> {
            try {
                InventoryItemEntity item = inventoryDao.getItemByName(itemName);
                if (listener != null) {
                    listener.onItemFetched(item);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching item: " + e.getMessage());
                if (listener != null) {
                    listener.onItemFetched(null);
                }
            }
        });
    }

    /**
     * Get low stock items.
     */
    public LiveData<List<InventoryItemEntity>> getLowStockItems(int threshold) {
        return inventoryDao.getLowStockItems(threshold);
    }

    /**
     * Search items by name.
     */
    public LiveData<List<InventoryItemEntity>> searchItems(String query) {
        return inventoryDao.searchItems("%" + query + "%");
    }

    /**
     * Delete all inventory items.
     */
    public void deleteAllItems() {
        executorService.execute(() -> {
            inventoryDao.deleteAllItems();
            Log.d(TAG, "All items deleted");
        });
    }

    // ========== USER OPERATIONS ==========

    /**
     * Authenticate user with username and password.
     */
    public void authenticateUser(String username, String password, OnAuthCompleteListener listener) {
        executorService.execute(() -> {
            try {
                String hashedPassword = hashPassword(password);
                String normalizedUsername = username.toLowerCase().trim();

                UserEntity user = userDao.authenticateUser(normalizedUsername, hashedPassword);

                if (listener != null) {
                    if (user != null) {
                        listener.onAuthComplete(user.getUserId(), user.getUsername());
                    } else {
                        listener.onAuthComplete(-1, null);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Authentication error: " + e.getMessage());
                if (listener != null) {
                    listener.onAuthComplete(-1, null);
                }
            }
        });
    }

    /**
     * Create a new user account.
     */
    public void createUser(String username, String password, OnUserCreateListener listener) {
        executorService.execute(() -> {
            try {
                String normalizedUsername = username.toLowerCase().trim();

                // Check if username exists
                int count = userDao.usernameExists(normalizedUsername);
                if (count > 0) {
                    if (listener != null) {
                        listener.onUserCreated(-2); // Username exists
                    }
                    return;
                }

                // Create new user
                String hashedPassword = hashPassword(password);
                UserEntity newUser = new UserEntity(normalizedUsername, hashedPassword);

                long userId = userDao.insertUser(newUser);

                if (listener != null) {
                    listener.onUserCreated(userId);
                }

                Log.d(TAG, "User created with ID: " + userId);
            } catch (Exception e) {
                Log.e(TAG, "Error creating user: " + e.getMessage());
                if (listener != null) {
                    listener.onUserCreated(-1);
                }
            }
        });
    }

    // ========== HELPER METHODS ==========

    /**
     * Hash password using SHA-256.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password: " + e.getMessage());
            return password; // Fallback - not recommended for production
        }
    }

    // ========== CALLBACK INTERFACES ==========

    public interface OnInsertCompleteListener {
        void onInsertComplete(long itemId);
    }

    public interface OnUpdateCompleteListener {
        void onUpdateComplete(boolean success);
    }

    public interface OnDeleteCompleteListener {
        void onDeleteComplete(boolean success);
    }

    public interface OnItemFetchListener {
        void onItemFetched(InventoryItemEntity item);
    }

    public interface OnAuthCompleteListener {
        void onAuthComplete(long userId, String username);
    }

    public interface OnUserCreateListener {
        void onUserCreated(long userId);
    }
}