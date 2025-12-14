package com.example.tristenbradneyinventoryapplication;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Room Database class for the Inventory Manager application.
 * This is the main database configuration with entities and version control.
 */
@Database(entities = {InventoryItemEntity.class, UserEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    // Abstract methods to get DAOs
    public abstract InventoryDao inventoryDao();
    public abstract UserDao userDao();

    /**
     * Singleton pattern to get database instance.
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "inventory_database"
                    )
                    .addCallback(roomCallback)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    /**
     * Callback to handle database creation and seeding.
     */
    private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            super.onCreate(db);

            // Database created successfully
        }
    };

    /**
     * Migration from version 2 to 3 (if needed).
     * This handles schema changes between database versions.
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            // Add schema changes here
        }
    };
}