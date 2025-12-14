package com.example.tristenbradneyinventoryapplication;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity for inventory items.
 * Represents the inventory table in the database with proper constraints.
 */
@Entity(tableName = "inventory_items",
        foreignKeys = {
                @ForeignKey(entity = UserEntity.class,
                        parentColumns = "user_id",
                        childColumns = "created_by",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = UserEntity.class,
                        parentColumns = "user_id",
                        childColumns = "last_modified_by",
                        onDelete = ForeignKey.SET_NULL)
        },
        indices = {
                @Index(value = "item_name", unique = true),
                @Index(value = "created_by"),
                @Index(value = "last_modified_by")
        })
public class InventoryItemEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    private long itemId;

    @ColumnInfo(name = "item_name")
    private String itemName;

    @ColumnInfo(name = "item_quantity")
    private int quantity;

    @ColumnInfo(name = "item_price")
    private double price;

    @ColumnInfo(name = "created_by")
    private long createdBy;

    @ColumnInfo(name = "last_modified_by")
    private long lastModifiedBy;

    @ColumnInfo(name = "created_date")
    private String createdDate;

    @ColumnInfo(name = "modified_date")
    private String modifiedDate;

    @ColumnInfo(name = "last_editor_username")
    private String lastEditorUsername;

    // Constructor
    public InventoryItemEntity(String itemName, int quantity, double price,
                               long createdBy, long lastModifiedBy) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
        this.createdBy = createdBy;
        this.lastModifiedBy = lastModifiedBy;
    }

    // Getters and Setters
    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(long createdBy) {
        this.createdBy = createdBy;
    }

    public long getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(long lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getLastEditorUsername() {
        return lastEditorUsername;
    }

    public void setLastEditorUsername(String lastEditorUsername) {
        this.lastEditorUsername = lastEditorUsername;
    }

    public double getTotalValue() {
        return quantity * price;
    }

    public boolean isLowStock(int threshold) {
        return quantity <= threshold;
    }
}