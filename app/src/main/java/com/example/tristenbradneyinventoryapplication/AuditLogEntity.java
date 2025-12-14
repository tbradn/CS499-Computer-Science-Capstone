package com.example.tristenbradneyinventoryapplication;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AuditLogEntity - Database entity for tracking all inventory changes
 *
 * ENHANCEMENT THREE: Database Security and Audit Trail
 *
 * This entity maintains a complete audit trail of all inventory operations,
 * providing accountability, security monitoring, and data recovery capabilities.
 *
 * Security Benefits:
 * - Tracks who made each change and when
 * - Records before and after values for updates
 * - Enables forensic analysis of unauthorized changes
 * - Supports compliance with data governance requirements
 *
 */
@Entity(
        tableName = "audit_log",
        foreignKeys = {
                @ForeignKey(
                        entity = InventoryItemEntity.class,
                        parentColumns = "id",
                        childColumns = "itemId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = UserEntity.class,
                        parentColumns = "id",
                        childColumns = "userId",
                        onDelete = ForeignKey.SET_NULL
                )
        },
        indices = {
                @Index(value = "itemId"),
                @Index(value = "userId"),
                @Index(value = "timestamp")
        }
)
public class AuditLogEntity {

    @PrimaryKey(autoGenerate = true)
    private long logId;

    // Foreign key to the inventory item that was changed
    private long itemId;

    // Foreign key to the user who made the change
    private long userId;

    // Type of action: INSERT, UPDATE, DELETE
    @NonNull
    private String action;

    // For UPDATE operations, track old and new values
    private Integer oldQuantity;
    private Integer newQuantity;
    private Double oldPrice;
    private Double newPrice;
    private String oldName;
    private String newName;

    // Timestamp of the change
    @NonNull
    private String timestamp;

    // Optional description or notes about the change
    private String changeDescription;

    /**
     * Constructor for audit log entries
     */
    public AuditLogEntity(long itemId, long userId, @NonNull String action) {
        this.itemId = itemId;
        this.userId = userId;
        this.action = action;
        this.timestamp = getCurrentTimestamp();
    }

    /**
     * Get current timestamp in ISO 8601 format
     */
    private static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * Create audit log for INSERT operation
     */
    public static AuditLogEntity createInsertLog(long itemId, long userId,
                                                 String itemName, int quantity, double price) {
        AuditLogEntity log = new AuditLogEntity(itemId, userId, "INSERT");
        log.setNewName(itemName);
        log.setNewQuantity(quantity);
        log.setNewPrice(price);
        log.setChangeDescription("Created new inventory item: " + itemName);
        return log;
    }

    /**
     * Create audit log for UPDATE operation
     */
    public static AuditLogEntity createUpdateLog(long itemId, long userId,
                                                 String oldName, String newName,
                                                 int oldQuantity, int newQuantity,
                                                 double oldPrice, double newPrice) {
        AuditLogEntity log = new AuditLogEntity(itemId, userId, "UPDATE");
        log.setOldName(oldName);
        log.setNewName(newName);
        log.setOldQuantity(oldQuantity);
        log.setNewQuantity(newQuantity);
        log.setOldPrice(oldPrice);
        log.setNewPrice(newPrice);

        // Build description of what changed
        StringBuilder desc = new StringBuilder("Updated: ");
        if (!oldName.equals(newName)) {
            desc.append(String.format("name '%s' → '%s', ", oldName, newName));
        }
        if (oldQuantity != newQuantity) {
            desc.append(String.format("quantity %d → %d, ", oldQuantity, newQuantity));
        }
        if (Math.abs(oldPrice - newPrice) > 0.001) {
            desc.append(String.format("price $%.2f → $%.2f, ", oldPrice, newPrice));
        }

        // Remove trailing comma and space
        String description = desc.toString();
        if (description.endsWith(", ")) {
            description = description.substring(0, description.length() - 2);
        }

        log.setChangeDescription(description);
        return log;
    }

    /**
     * Create audit log for DELETE operation
     */
    public static AuditLogEntity createDeleteLog(long itemId, long userId,
                                                 String itemName, int quantity, double price) {
        AuditLogEntity log = new AuditLogEntity(itemId, userId, "DELETE");
        log.setOldName(itemName);
        log.setOldQuantity(quantity);
        log.setOldPrice(price);
        log.setChangeDescription("Deleted inventory item: " + itemName);
        return log;
    }

    // ========== GETTERS AND SETTERS ==========

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @NonNull
    public String getAction() {
        return action;
    }

    public void setAction(@NonNull String action) {
        this.action = action;
    }

    public Integer getOldQuantity() {
        return oldQuantity;
    }

    public void setOldQuantity(Integer oldQuantity) {
        this.oldQuantity = oldQuantity;
    }

    public Integer getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(Integer newQuantity) {
        this.newQuantity = newQuantity;
    }

    public Double getOldPrice() {
        return oldPrice;
    }

    public void setOldPrice(Double oldPrice) {
        this.oldPrice = oldPrice;
    }

    public Double getNewPrice() {
        return newPrice;
    }

    public void setNewPrice(Double newPrice) {
        this.newPrice = newPrice;
    }

    public String getOldName() {
        return oldName;
    }

    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    @NonNull
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(@NonNull String timestamp) {
        this.timestamp = timestamp;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }

    /**
     * Get a formatted summary of this audit entry
     */
    public String getFormattedSummary() {
        return String.format("[%s] %s by User %d: %s",
                timestamp, action, userId,
                changeDescription != null ? changeDescription : "No description");
    }

    @Override
    public String toString() {
        return "AuditLogEntity{" +
                "logId=" + logId +
                ", itemId=" + itemId +
                ", userId=" + userId +
                ", action='" + action + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", description='" + changeDescription + '\'' +
                '}';
    }
}