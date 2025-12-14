package com.example.tristenbradneyinventoryapplication;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object (DAO) for inventory operations.
 * Room will implement these methods automatically.
 */
@Dao
public interface InventoryDao {

    /**
     * Insert a new inventory item.
     * @return The row ID of the newly inserted item
     */
    @Insert
    long insertItem(InventoryItemEntity item);

    /**
     * Update an existing inventory item.
     * @return Number of rows updated
     */
    @Update
    int updateItem(InventoryItemEntity item);

    /**
     * Delete an inventory item.
     * @return Number of rows deleted
     */
    @Delete
    int deleteItem(InventoryItemEntity item);

    /**
     * Get all inventory items with user information.
     * Returns LiveData for automatic UI updates.
     */
    @Query("SELECT i.*, u.username as last_editor_username " +
            "FROM inventory_items i " +
            "LEFT JOIN users u ON i.last_modified_by = u.user_id " +
            "ORDER BY i.modified_date DESC")
    LiveData<List<InventoryItemEntity>> getAllItems();

    /**
     * Get a specific item by ID.
     */
    @Query("SELECT * FROM inventory_items WHERE item_id = :itemId")
    InventoryItemEntity getItemById(long itemId);

    /**
     * Get a specific item by name.
     */
    @Query("SELECT * FROM inventory_items WHERE item_name = :itemName")
    InventoryItemEntity getItemByName(String itemName);

    /**
     * Get all low-stock items.
     */
    @Query("SELECT * FROM inventory_items WHERE item_quantity <= :threshold")
    LiveData<List<InventoryItemEntity>> getLowStockItems(int threshold);

    /**
     * Delete all inventory items.
     */
    @Query("DELETE FROM inventory_items")
    void deleteAllItems();

    /**
     * Search items by name.
     */
    @Query("SELECT * FROM inventory_items WHERE item_name LIKE :searchQuery ORDER BY item_name")
    LiveData<List<InventoryItemEntity>> searchItems(String searchQuery);
}