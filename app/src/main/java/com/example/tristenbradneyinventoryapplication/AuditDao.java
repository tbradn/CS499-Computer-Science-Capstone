package com.example.tristenbradneyinventoryapplication;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * AuditDao - Data Access Object for audit log operations
 */
@Dao
public interface AuditDao {

    /**
     * Insert a new audit log entry
     */
    @Insert
    long insert(AuditLogEntity auditLog);

    /**
     * Get all audit history for a specific inventory item
     */
    @Query("SELECT * FROM audit_log WHERE itemId = :itemId ORDER BY timestamp DESC")
    LiveData<List<AuditLogEntity>> getAuditHistoryForItem(long itemId);

    /**
     * Get all audit history for a specific user
     */
    @Query("SELECT * FROM audit_log WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<AuditLogEntity>> getAuditHistoryForUser(long userId);

    /**
     * Get recent audit logs (last 100 entries)
     */
    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT 100")
    LiveData<List<AuditLogEntity>> getRecentAuditLogs();

    /**
     * Get audit logs for a specific action type (INSERT, UPDATE, DELETE)
     */
    @Query("SELECT * FROM audit_log WHERE action = :action ORDER BY timestamp DESC")
    LiveData<List<AuditLogEntity>> getAuditLogsByAction(String action);

    /**
     * Get count of all audit log entries
     */
    @Query("SELECT COUNT(*) FROM audit_log")
    int getAuditLogCount();

    /**
     * Get count of audit logs for a specific item
     */
    @Query("SELECT COUNT(*) FROM audit_log WHERE itemId = :itemId")
    int getAuditLogCountForItem(long itemId);

    /**
     * Delete all audit logs (use with caution - only for testing)
     */
    @Query("DELETE FROM audit_log")
    int deleteAllAuditLogs();

    /**
     * Get audit logs within a date range
     */
    @Query("SELECT * FROM audit_log WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    LiveData<List<AuditLogEntity>> getAuditLogsByDateRange(String startDate, String endDate);
}