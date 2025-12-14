package com.example.tristenbradneyinventoryapplication;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * InventoryRepository - Enhanced with audit trail logging
 *
 * ENHANCEMENT ONE: MVVM Architecture with Room Database
 * ENHANCEMENT THREE: Database Security and Audit Trail
 *
 * This repository now logs all database operations to the audit trail,
 * providing complete accountability for all data changes.
 *
 * All database operations are performed on a background thread to prevent
 * blocking the UI thread.
 *
 * @author Tristen Bradney
 * @version 3.0
 * @since 2025-11-23
 */
public class InventoryRepository {

    private final InventoryDao inventoryDao;
    private final UserDao userDao;
    private final AuditDao auditDao;
    private final LiveData<List<InventoryItemEntity>> allItems;
    private final ExecutorService executorService;

    /**
     * Constructor - initializes DAOs and executor service
     */
    public InventoryRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        inventoryDao = database.inventoryDao();
        userDao = database.userDao();
        auditDao = database.auditDao();
        allItems = inventoryDao.getAllItems();

        // Create thread pool for background operations
        executorService = Executors.newFixedThreadPool(4);
    }

    // ========== INVENTORY OPERATIONS WITH AUDIT LOGGING ==========

    /**
     * Insert a new inventory item with audit logging
     *
     * ENHANCEMENT THREE: Logs item creation to audit trail
     *
     * @param item The inventory item to insert
     * @param userId The ID of the user creating the item
     * @param callback Callback to receive the result
     */
    public void insert(InventoryItemEntity item, long userId, RepositoryCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                // Set audit fields
                item.setCreatedBy(userId);
                item.setLastModifiedBy(userId);

                // Insert the item
                long itemId = inventoryDao.insert(item);

                if (itemId > 0) {
                    // Update the item's ID
                    item.setId(itemId);

                    // Create audit log entry
                    AuditLogEntity auditLog = AuditLogEntity.createInsertLog(
                            itemId,
                            userId,
                            item.getItemName(),
                            item.getQuantity(),
                            item.getPrice()
                    );
                    auditDao.insert(auditLog);

                    // Success callback
                    if (callback != null) {
                        callback.onSuccess(itemId);
                    }
                } else {
                    // Failure callback
                    if (callback != null) {
                        callback.onError("Failed to insert item");
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Database error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Update an existing inventory item with audit logging
     *
     * ENHANCEMENT THREE: Logs all changes to audit trail
     *
     * @param item The inventory item to update (with old values)
     * @param newQuantity The new quantity
     * @param newPrice The new price
     * @param userId The ID of the user making the change
     * @param callback Callback to receive the result
     */
    public void update(InventoryItemEntity item, int newQuantity, double newPrice,
                       long userId, RepositoryCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                // Store old values for audit log
                String oldName = item.getItemName();
                int oldQuantity = item.getQuantity();
                double oldPrice = item.getPrice();

                // Update item with new values
                item.setQuantity(newQuantity);
                item.setPrice(newPrice);
                item.setLastModifiedBy(userId);
                item.updateLastModifiedDate();

                // Update in database
                inventoryDao.update(item);

                // Create audit log entry
                AuditLogEntity auditLog = AuditLogEntity.createUpdateLog(
                        item.getId(),
                        userId,
                        oldName,
                        item.getItemName(),
                        oldQuantity,
                        newQuantity,
                        oldPrice,
                        newPrice
                );
                auditDao.insert(auditLog);

                // Success callback
                if (callback != null) {
                    callback.onSuccess(true);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Database error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Update only item name with audit logging
     */
    public void updateName(InventoryItemEntity item, String newName, long userId,
                           RepositoryCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                String oldName = item.getItemName();

                item.setItemName(newName);
                item.setLastModifiedBy(userId);
                item.updateLastModifiedDate();

                inventoryDao.update(item);

                // Create audit log
                AuditLogEntity auditLog = AuditLogEntity.createUpdateLog(
                        item.getId(), userId,
                        oldName, newName,
                        item.getQuantity(), item.getQuantity(),
                        item.getPrice(), item.getPrice()
                );
                auditDao.insert(auditLog);

                if (callback != null) {
                    callback.onSuccess(true);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Database error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Delete an inventory item with audit logging
     *
     * ENHANCEMENT THREE: Logs item deletion to audit trail
     *
     * @param item The inventory item to delete
     * @param userId The ID of the user deleting the item
     * @param callback Callback to receive the result
     */
    public void delete(InventoryItemEntity item, long userId, RepositoryCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                // Create audit log BEFORE deleting (so we have the data)
                AuditLogEntity auditLog = AuditLogEntity.createDeleteLog(
                        item.getId(),
                        userId,
                        item.getItemName(),
                        item.getQuantity(),
                        item.getPrice()
                );
                auditDao.insert(auditLog);

                // Delete the item
                inventoryDao.delete(item);

                // Success callback
                if (callback != null) {
                    callback.onSuccess(true);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Database error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Delete all inventory items (use with caution)
     *
     * ENHANCEMENT THREE: Logs mass deletion to audit trail
     */
    public void deleteAllItems(long userId, RepositoryCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                // Get all items before deleting for audit log
                List<InventoryItemEntity> items = inventoryDao.getAllItemsSync();

                // Log each deletion
                for (InventoryItemEntity item : items) {
                    AuditLogEntity auditLog = AuditLogEntity.createDeleteLog(
                            item.getId(),
                            userId,
                            item.getItemName(),
                            item.getQuantity(),
                            item.getPrice()
                    );
                    auditDao.insert(auditLog);
                }

                // Delete all items
                inventoryDao.deleteAllItems();

                if (callback != null) {
                    callback.onSuccess(true);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Database error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Get all inventory items
     * Returns LiveData for automatic UI updates
     */
    public LiveData<List<InventoryItemEntity>> getAllItems() {
        return allItems;
    }

    /**
     * Get inventory item by ID
     */
    public void getItemById(long itemId, RepositoryCallback<InventoryItemEntity> callback) {
        executorService.execute(() -> {
            try {
                InventoryItemEntity item = inventoryDao.getItemByIdSync(itemId);
                if (callback != null) {
                    if (item != null) {
                        callback.onSuccess(item);
                    } else {
                        callback.onError("Item not found");
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Database error: " + e.getMessage());
                }
            }
        });
    }

    // ========== USER OPERATIONS ==========

    /**
     * Authenticate user credentials
     *
     * SECURITY: Uses parameterized queries via Room to prevent SQL injection
     */
    public void authenticateUser(String username, String password,
                                 AuthCallback callback) {
        executorService.execute(() -> {
            try {
                UserEntity user = userDao.getUserByUsernameSync(username);

                if (user != null && user.getPassword().equals(password)) {
                    // Authentication successful
                    if (callback != null) {
                        callback.onAuthComplete(user.getId(), user.getUsername());
                    }
                } else {
                    // Authentication failed
                    if (callback != null) {
                        callback.onAuthComplete(-1, null);
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onAuthComplete(-1, null);
                }
            }
        });
    }

    /**
     * Create a new user account
     *
     * SECURITY: Validates input and uses parameterized queries
     */
    public void createUser(String username, String password,
                           UserCreateCallback callback) {
        executorService.execute(() -> {
            try {
                // Check if username already exists
                UserEntity existingUser = userDao.getUserByUsernameSync(username);

                if (existingUser != null) {
                    // Username already exists
                    if (callback != null) {
                        callback.onUserCreated(-2); // -2 indicates duplicate username
                    }
                    return;
                }

                // Create new user
                UserEntity newUser = new UserEntity(username, password);
                long userId = userDao.insert(newUser);

                if (callback != null) {
                    callback.onUserCreated(userId);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onUserCreated(-1); // -1 indicates error
                }
            }
        });
    }

    /**
     * Get username by user ID
     */
    public void getUsernameById(long userId, RepositoryCallback<String> callback) {
        executorService.execute(() -> {
            try {
                String username = userDao.getUsernameByIdSync(userId);
                if (callback != null) {
                    callback.onSuccess(username != null ? username : "Unknown");
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Database error: " + e.getMessage());
                }
            }
        });
    }

    // ========== AUDIT OPERATIONS ==========

    /**
     * Get audit history for a specific item
     */
    public LiveData<List<AuditLogEntity>> getAuditHistoryForItem(long itemId) {
        return auditDao.getAuditHistoryForItem(itemId);
    }

    /**
     * Get audit history for a specific user
     */
    public LiveData<List<AuditLogEntity>> getAuditHistoryForUser(long userId) {
        return auditDao.getAuditHistoryForUser(userId);
    }

    /**
     * Get recent audit logs (last 100 entries)
     */
    public LiveData<List<AuditLogEntity>> getRecentAuditLogs() {
        return auditDao.getRecentAuditLogs();
    }

    /**
     * Get total count of audit log entries
     */
    public void getAuditLogCount(RepositoryCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int count = auditDao.getAuditLogCount();
                if (callback != null) {
                    callback.onSuccess(count);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Database error: " + e.getMessage());
                }
            }
        });
    }

    // ========== CALLBACK INTERFACES ==========

    /**
     * Generic callback interface for repository operations
     */
    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    /**
     * Callback interface for user authentication
     */
    public interface AuthCallback {
        void onAuthComplete(long userId, String username);
    }

    /**
     * Callback interface for user creation
     */
    public interface UserCreateCallback {
        void onUserCreated(long userId);
    }

    /**
     * Shutdown executor service
     * Call this when app is closing
     */
    public void shutdown() {
        executorService.shutdown();
    }
}