package com.example.tristenbradneyinventoryapplication;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * InventoryItemEntity - Enhanced database entity with audit trail support
 *
 * ENHANCEMENT ONE: MVVM Architecture with Room Database
 * ENHANCEMENT THREE: Database Security and Audit Trail
 *
 * This entity now includes:
 * - User tracking (who created/modified items)
 * - Timestamp tracking (when items were created/modified)
 * - Database constraints for data integrity
 *
 */
@Entity(
        tableName = "inventory_items",
        foreignKeys = {
                @ForeignKey(
                        entity = UserEntity.class,
                        parentColumns = "id",
                        childColumns = "createdBy",
                        onDelete = ForeignKey.SET_NULL
                ),
                @ForeignKey(
                        entity = UserEntity.class,
                        parentColumns = "id",
                        childColumns = "lastModifiedBy",
                        onDelete = ForeignKey.SET_NULL
                )
        },
        indices = {
                @Index(value = "createdBy"),
                @Index(value = "lastModifiedBy"),
                @Index(value = "itemName")
        }
)
public class InventoryItemEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String itemName;

    private int quantity;

    private double price;

    // ENHANCEMENT THREE: Audit trail fields
    private long createdBy;
    private Long lastModifiedBy;

    @NonNull
    private String createdDate;

    private String lastModifiedDate;

    // For display purposes - not stored in database
    private transient String lastEditorUsername;

    /**
     * Default constructor required by Room
     */
    public InventoryItemEntity() {
        this.createdDate = getCurrentTimestamp();
    }

    /**
     * Constructor with basic fields
     */
    public InventoryItemEntity(@NonNull String itemName, int quantity, double price) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
        this.createdDate = getCurrentTimestamp();
    }

    /**
     * Constructor with full audit trail fields
     */
    public InventoryItemEntity(@NonNull String itemName, int quantity, double price, long createdBy) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
        this.createdBy = createdBy;
        this.createdDate = getCurrentTimestamp();
    }

    /**
     * Get current timestamp in ISO 8601 format
     */
    private static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * Update the last modified timestamp
     */
    public void updateLastModifiedDate() {
        this.lastModifiedDate = getCurrentTimestamp();
    }

    // ========== BUSINESS LOGIC METHODS ==========

    /**
     * Check if item is low on stock
     */
    public boolean isLowStock(int threshold) {
        return quantity <= threshold;
    }

    /**
     * Get total value of this item (quantity * price)
     */
    public double getTotalValue() {
        return quantity * price;
    }

    /**
     * Get formatted price string
     */
    public String getFormattedPrice() {
        return NumberFormat.getCurrencyInstance(Locale.US).format(price);
    }

    /**
     * Get formatted total value string
     */
    public String getFormattedTotalValue() {
        return NumberFormat.getCurrencyInstance(Locale.US).format(getTotalValue());
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validate item name
     * ENHANCEMENT THREE: Input validation for security
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        if (name.length() > 100) {
            return false;
        }
        return true;
    }

    /**
     * Validate quantity
     * ENHANCEMENT THREE: Input validation for security
     */
    public static boolean isValidQuantity(int quantity) {
        return quantity >= 0;
    }

    /**
     * Validate price
     * ENHANCEMENT THREE: Input validation for security
     */
    public static boolean isValidPrice(double price) {
        return price >= 0;
    }

    // ========== GETTERS AND SETTERS ==========

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getItemName() {
        return itemName;
    }

    public void setItemName(@NonNull String itemName) {
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

    public Long getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(Long lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @NonNull
    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(@NonNull String createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getLastEditorUsername() {
        return lastEditorUsername;
    }

    public void setLastEditorUsername(String lastEditorUsername) {
        this.lastEditorUsername = lastEditorUsername;
    }

    @Override
    public String toString() {
        return "InventoryItemEntity{" +
                "id=" + id +
                ", itemName='" + itemName + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", createdBy=" + createdBy +
                ", lastModifiedBy=" + lastModifiedBy +
                ", createdDate='" + createdDate + '\'' +
                ", lastModifiedDate='" + lastModifiedDate + '\'' +
                '}';
    }
}