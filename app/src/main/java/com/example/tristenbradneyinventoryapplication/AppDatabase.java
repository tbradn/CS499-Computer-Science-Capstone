package com.example.tristenbradneyinventoryapplication;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

/**
 * AppDatabase - Room Database configuration with audit trail support
 *
 * ENHANCEMENT ONE: MVVM Architecture with Room Database
 * ENHANCEMENT THREE: Database Security and Audit Trail
 *
 * Database Version History:
 * - Version 1: Initial database with InventoryItemEntity and UserEntity
 * - Version 2: Added AuditLogEntity and audit trail fields
 *
 * Security Features:
 * - Foreign key constraints enforce referential integrity
 * - Parameterized queries prevent SQL injection
 * - Audit trail tracks all data changes
 *
 */
@Database(
        entities = {
                InventoryItemEntity.class,
                UserEntity.class,
                AuditLogEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    // DAOs
    public abstract InventoryDao inventoryDao();
    public abstract UserDao userDao();
    public abstract AuditDao auditDao();

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    /**
     * Get singleton instance of database
     * Thread-safe using double-checked locking
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "inventory_database"
                            )
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration() // For development
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Migration from version 1 to version 2
     * Adds audit trail functionality
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add audit trail fields to inventory_items table
            database.execSQL(
                    "ALTER TABLE inventory_items ADD COLUMN createdBy INTEGER NOT NULL DEFAULT 0"
            );
            database.execSQL(
                    "ALTER TABLE inventory_items ADD COLUMN lastModifiedBy INTEGER"
            );
            database.execSQL(
                    "ALTER TABLE inventory_items ADD COLUMN createdDate TEXT NOT NULL DEFAULT '2025-01-01 00:00:00'"
            );
            database.execSQL(
                    "ALTER TABLE inventory_items ADD COLUMN lastModifiedDate TEXT"
            );

            // Create audit_log table
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS audit_log (" +
                            "logId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "itemId INTEGER NOT NULL, " +
                            "userId INTEGER NOT NULL, " +
                            "action TEXT NOT NULL, " +
                            "oldQuantity INTEGER, " +
                            "newQuantity INTEGER, " +
                            "oldPrice REAL, " +
                            "newPrice REAL, " +
                            "oldName TEXT, " +
                            "newName TEXT, " +
                            "timestamp TEXT NOT NULL, " +
                            "changeDescription TEXT, " +
                            "FOREIGN KEY(itemId) REFERENCES inventory_items(id) ON DELETE CASCADE, " +
                            "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE SET NULL" +
                            ")"
            );

            // Create indices for better query performance
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_audit_log_itemId ON audit_log(itemId)"
            );
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_audit_log_userId ON audit_log(userId)"
            );
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_audit_log_timestamp ON audit_log(timestamp)"
            );
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_inventory_items_createdBy ON inventory_items(createdBy)"
            );
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_inventory_items_lastModifiedBy ON inventory_items(lastModifiedBy)"
            );
        }
    };

    /**
     * Close database instance
     * Should only be called when app is completely shutting down
     */
    public static void closeDatabase() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}