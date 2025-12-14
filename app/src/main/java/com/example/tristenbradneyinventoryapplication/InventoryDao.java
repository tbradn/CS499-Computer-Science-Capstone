package com.example.tristenbradneyinventoryapplication;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * InventoryDao - Data Access Object with enhanced security
 *
 * ENHANCEMENT ONE: MVVM Architecture with Room Database
 * ENHANCEMENT THREE: Database Security - All queries are parameterized
 *
 * Room automatically generates safe, parameterized SQL queries from these methods,
 * preventing SQL injection attacks. All user inputs are properly escaped.
 *
 * @author Tristen Bradney
 * @version 3.0
 * @since 2025-11-23
 */
@Dao
public interface InventoryDao {

    /**
     * Insert a new inventory item
     *
     * SECURITY: Room uses parameterized insertion to prevent SQL injection
     *
     * @param item The inventory item to insert
     * @return The ID of the inserted item
     */
    @Insert
    long insert(InventoryItemEntity item);

    /**
     * Update an existing inventory item
     *
     * SECURITY: Room uses parameterized update to prevent SQL injection
     *
     * @param item The inventory item to update
     */
    @Update
    void update(InventoryItemEntity item);

    /**
     * Delete an inventory item
     *
     * @param item The inventory item to delete
     */
    @Delete
    void delete(InventoryItemEntity item);

    /**
     * Delete all inventory items
     *
     * @return Number of items deleted
     */
    @Query("DELETE FROM inventory_items")
    int deleteAllItems();

    /**
     * Get all inventory items with LiveData for automatic UI updates
     *
     * @return LiveData list of all inventory items
     */
    @Query("SELECT * FROM inventory_items ORDER BY itemName ASC")
    LiveData<List<InventoryItemEntity>> getAllItems();

    /**
     * Get all inventory items synchronously (for background operations)
     *
     * @return List of all inventory items
     */
    @Query("SELECT * FROM inventory_items ORDER BY itemName ASC")
    List<InventoryItemEntity> getAllItemsSync();

    /**
     * Get inventory item by ID with LiveData
     *
     * SECURITY: Uses parameterized query to prevent SQL injection
     *
     * @param itemId The ID of the item
     * @return LiveData of the inventory item
     */
    @Query("SELECT * FROM inventory_items WHERE id = :itemId")
    LiveData<InventoryItemEntity> getItemById(long itemId);

    /**
     * Get inventory item by ID synchronously
     *
     * SECURITY: Uses parameterized query to prevent SQL injection
     *
     * @param itemId The ID of the item
     * @return The inventory item
     */
    @Query("SELECT * FROM inventory_items WHERE id = :itemId")
    InventoryItemEntity getItemByIdSync(long itemId);

    /**
     * Search inventory items by name (case-insensitive)
     *
     * SECURITY: Uses parameterized query to prevent SQL injection
     *
     * @param searchTerm The search term
     * @return LiveData list of matching items
     */
    @Query("SELECT * FROM inventory_items WHERE itemName LIKE '%' || :searchTerm || '%' ORDER BY itemName ASC")
    LiveData<List<InventoryItemEntity>> searchItemsByName(String searchTerm);

    /**
     * Get items with low stock (quantity <= threshold)
     *
     * SECURITY: Uses parameterized query to prevent SQL injection
     *
     * @param threshold The low stock threshold
     * @return LiveData list of low-stock items
     */
    @Query("SELECT * FROM inventory_items WHERE quantity <= :threshold ORDER BY quantity ASC")
    LiveData<List<InventoryItemEntity>> getLowStockItems(int threshold);

    /**
     * Get count of all inventory items
     *
     * @return Total number of items
     */
    @Query("SELECT COUNT(*) FROM inventory_items")
    int getItemCount();

    /**
     * Get items created by a specific user
     *
     * SECURITY: Uses parameterized query to prevent SQL injection
     *
     * @param userId The ID of the user
     * @return LiveData list of items created by the user
     */
    @Query("SELECT * FROM inventory_items WHERE createdBy = :userId ORDER BY createdDate DESC")
    LiveData<List<InventoryItemEntity>> getItemsCreatedByUser(long userId);

    /**
     * Get items last modified by a specific user
     *
     * SECURITY: Uses parameterized query to prevent SQL injection
     *
     * @param userId The ID of the user
     * @return LiveData list of items last modified by the user
     */
    @Query("SELECT * FROM inventory_items WHERE lastModifiedBy = :userId ORDER BY lastModifiedDate DESC")
    LiveData<List<InventoryItemEntity>> getItemsModifiedByUser(long userId);
}