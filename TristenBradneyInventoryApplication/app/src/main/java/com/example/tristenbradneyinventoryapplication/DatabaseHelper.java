package com.example.tristenbradneyinventoryapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper class handles all SQLite database operations for the inventory application.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database configuration constants.
    private static final String DATABASE_NAME = "InventoryDatabase.db";

    // Increased version for schema change.
    private static final int DATABASE_VERSION = 2;
    private static final String TAG = "DatabaseHelper";

    // Users table constants.
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD_HASH = "password_hash";
    private static final String COLUMN_CREATED_DATE = "created_date";

    // Inventory table constants.
    private static final String TABLE_INVENTORY = "inventory";
    private static final String COLUMN_ITEM_ID = "item_id";
    private static final String COLUMN_ITEM_NAME = "item_name";
    private static final String COLUMN_ITEM_QUANTITY = "item_quantity";
    private static final String COLUMN_ITEM_PRICE = "item_price";

    // Track who created the item.
    private static final String COLUMN_ITEM_CREATED_BY = "created_by";

    // Track who last modified.
    private static final String COLUMN_ITEM_LAST_MODIFIED_BY = "last_modified_by";
    private static final String COLUMN_ITEM_CREATED_DATE = "created_date";
    private static final String COLUMN_ITEM_MODIFIED_DATE = "modified_date";

    // SQL statements for table creation.
    private static final String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
            COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, " +
            COLUMN_PASSWORD_HASH + " TEXT NOT NULL, " +
            COLUMN_CREATED_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP)";

    // Modified inventory table - removed user_id foreign key constraint for shared access.
    private static final String CREATE_INVENTORY_TABLE = "CREATE TABLE " + TABLE_INVENTORY + " (" +
            COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

            // Made item name unique across all users.
            COLUMN_ITEM_NAME + " TEXT NOT NULL UNIQUE, " +
            COLUMN_ITEM_QUANTITY + " INTEGER NOT NULL DEFAULT 0, " +
            COLUMN_ITEM_PRICE + " REAL NOT NULL DEFAULT 0.0, " +
            COLUMN_ITEM_CREATED_BY + " INTEGER NOT NULL, " +
            COLUMN_ITEM_LAST_MODIFIED_BY + " INTEGER NOT NULL, " +
            COLUMN_ITEM_CREATED_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_ITEM_MODIFIED_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_ITEM_CREATED_BY + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "), " +
            "FOREIGN KEY(" + COLUMN_ITEM_LAST_MODIFIED_BY + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "))";

    private static DatabaseHelper instance;

    /**
     * Singleton pattern implementation.
     */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {

            // Create users table.
            db.execSQL(CREATE_USERS_TABLE);
            Log.d(TAG, "Users table created successfully");

            // Create shared inventory table.
            db.execSQL(CREATE_INVENTORY_TABLE);
            Log.d(TAG, "Shared inventory table created successfully");

            // No longer insert default user or sample items.
            Log.d(TAG, "Database created with empty tables - no default data");

        } catch (Exception e) {
            Log.e(TAG, "Error creating database tables: " + e.getMessage(), e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

            if (oldVersion < 2) {

                // Migrate from user-specific to shared inventory.
                migrateToSharedInventory(db);
            } else {

                // For major upgrades, recreate tables.
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
                onCreate(db);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error upgrading database: " + e.getMessage(), e);

            // If migration fails, recreate everything.
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }
    }

    /**
     * Migrate from user-specific inventory to shared inventory.
     */
    private void migrateToSharedInventory(SQLiteDatabase db) {
        try {
            Log.d(TAG, "Migrating to shared inventory system...");

            // Create temporary table with new schema.
            String tempTableName = TABLE_INVENTORY + "_temp";
            String createTempTable = "CREATE TABLE " + tempTableName + " (" +
                    COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ITEM_NAME + " TEXT NOT NULL UNIQUE, " +
                    COLUMN_ITEM_QUANTITY + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_ITEM_PRICE + " REAL NOT NULL DEFAULT 0.0, " +
                    COLUMN_ITEM_CREATED_BY + " INTEGER NOT NULL, " +
                    COLUMN_ITEM_LAST_MODIFIED_BY + " INTEGER NOT NULL, " +
                    COLUMN_ITEM_CREATED_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    COLUMN_ITEM_MODIFIED_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP)";

            db.execSQL(createTempTable);

            // Copy existing data to temp table.
            String copyData = "INSERT OR REPLACE INTO " + tempTableName + " " +
                    "(" + COLUMN_ITEM_NAME + ", " + COLUMN_ITEM_QUANTITY + ", " + COLUMN_ITEM_PRICE + ", " +
                    COLUMN_ITEM_CREATED_BY + ", " + COLUMN_ITEM_LAST_MODIFIED_BY + ") " +
                    "SELECT " + COLUMN_ITEM_NAME + ", SUM(" + COLUMN_ITEM_QUANTITY + "), " +
                    "AVG(" + COLUMN_ITEM_PRICE + "), MIN(user_id), MAX(user_id) " +
                    "FROM " + TABLE_INVENTORY + " GROUP BY " + COLUMN_ITEM_NAME;

            db.execSQL(copyData);

            // Drop old table and rename temp table.
            db.execSQL("DROP TABLE " + TABLE_INVENTORY);
            db.execSQL("ALTER TABLE " + tempTableName + " RENAME TO " + TABLE_INVENTORY);

            Log.d(TAG, "Successfully migrated to shared inventory system");

        } catch (Exception e) {
            Log.e(TAG, "Error during migration: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Creates a default user for testing.
     */
    private void insertDefaultUser(SQLiteDatabase db) {
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_USERNAME, "admin");
            values.put(COLUMN_PASSWORD_HASH, hashPassword("admin123"));

            long result = db.insert(TABLE_USERS, null, values);
            if (result != -1) {
                Log.d(TAG, "Default user 'admin' created with password 'admin123'");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating default user: " + e.getMessage(), e);
        }
    }

    /**
     * Insert sample inventory items for demonstration.
     */
    private void insertSampleInventoryItems(SQLiteDatabase db) {
        try {

            // Get admin user ID for sample items.
            // Default admin user should have ID 1.
            long adminUserId = 1;

            String[][] sampleItems = {
                    {"Office Supplies - Pens", "25", "2.99"},
                    {"Office Supplies - Paper", "100", "8.50"},
                    {"Electronics - USB Cables", "15", "12.99"},
                    {"Cleaning - Disinfectant", "8", "4.75"}
            };

            for (String[] item : sampleItems) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_ITEM_NAME, item[0]);
                values.put(COLUMN_ITEM_QUANTITY, Integer.parseInt(item[1]));
                values.put(COLUMN_ITEM_PRICE, Double.parseDouble(item[2]));
                values.put(COLUMN_ITEM_CREATED_BY, adminUserId);
                values.put(COLUMN_ITEM_LAST_MODIFIED_BY, adminUserId);

                db.insert(TABLE_INVENTORY, null, values);
            }

            Log.d(TAG, "Sample inventory items created");

        } catch (Exception e) {
            Log.e(TAG, "Error creating sample items: " + e.getMessage(), e);
        }
    }

    /**
     * Authenticates a user with username and password.
     */
    public long authenticateUser(String username, String password) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            String hashedPassword = hashPassword(password);

            String[] columns = {COLUMN_USER_ID};
            String selection = COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD_HASH + " = ?";
            String[] selectionArgs = {username.toLowerCase().trim(), hashedPassword};

            cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                long userId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
                Log.d(TAG, "User authenticated successfully: " + username);
                return userId;
            } else {
                Log.d(TAG, "Authentication failed for user: " + username);
                return -1;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error authenticating user: " + e.getMessage(), e);
            return -1;
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    /**
     * Creates a new user account.
     */
    public long createUser(String username, String password) {
        SQLiteDatabase db = null;

        try {
            String normalizedUsername = username.toLowerCase().trim();

            Log.d(TAG, "Attempting to create user: " + normalizedUsername);

            // Check if username already exists.
            if (isUsernameExists(normalizedUsername)) {
                Log.d(TAG, "Username already exists: " + normalizedUsername);

                // Special return code for existing username.
                return -2;
            }

            db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(COLUMN_USERNAME, normalizedUsername);
            values.put(COLUMN_PASSWORD_HASH, hashPassword(password));

            long userId = db.insert(TABLE_USERS, null, values);

            if (userId != -1) {
                Log.d(TAG, "User created successfully: " + normalizedUsername + " with ID: " + userId);
            } else {
                Log.e(TAG, "Failed to create user: " + normalizedUsername);
            }

            return userId;

        } catch (Exception e) {
            Log.e(TAG, "Error creating user: " + e.getMessage(), e);
            return -1;
        } finally {
            if (db != null) db.close();
        }
    }

    /**
     * Checks if a username already exists.
     */
    private boolean isUsernameExists(String username) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();

            String[] columns = {COLUMN_USER_ID};
            String selection = COLUMN_USERNAME + " = ?";
            String[] selectionArgs = {username.toLowerCase().trim()};

            cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);

            boolean exists = cursor != null && cursor.getCount() > 0;
            Log.d(TAG, "Username '" + username + "' exists: " + exists);
            return exists;

        } catch (Exception e) {
            Log.e(TAG, "Error checking username existence: " + e.getMessage(), e);
            return false;
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    /**
     * Adds a new inventory item to the shared database.
     */
    public long addInventoryItem(InventoryActivity.InventoryItem item, long userId) {
        SQLiteDatabase db = null;

        try {
            Log.d(TAG, "addInventoryItem called with userId: " + userId + ", itemName: " + item.getName());

            db = this.getWritableDatabase();

            // Check if item with this name already exists using the same database connection.
            String[] columns = {COLUMN_ITEM_ID};
            String selection = COLUMN_ITEM_NAME + " = ?";
            String[] selectionArgs = {item.getName().trim()};

            Cursor cursor = db.query(TABLE_INVENTORY, columns, selection, selectionArgs, null, null, null);
            boolean exists = cursor != null && cursor.getCount() > 0;
            cursor.close();

            if (exists) {
                Log.d(TAG, "Item with name '" + item.getName() + "' already exists");
                // Special return code for existing item name.
                return -2;
            }

            ContentValues values = new ContentValues();
            values.put(COLUMN_ITEM_NAME, item.getName());
            values.put(COLUMN_ITEM_QUANTITY, item.getQuantity());
            values.put(COLUMN_ITEM_PRICE, item.getPrice());
            values.put(COLUMN_ITEM_CREATED_BY, userId);
            values.put(COLUMN_ITEM_LAST_MODIFIED_BY, userId);

            Log.d(TAG, "Inserting item with values: " + values.toString());

            long itemId = db.insert(TABLE_INVENTORY, null, values);

            if (itemId != -1) {
                Log.d(TAG, "Inventory item added successfully with ID: " + itemId);
            } else {
                Log.e(TAG, "Failed to add inventory item - insert returned -1");
            }

            return itemId;

        } catch (Exception e) {
            Log.e(TAG, "Error adding inventory item: " + e.getMessage(), e);
            return -1;
        } finally {
            if (db != null) db.close();
        }
    }

    /**
     * Check if an item name already exists in the shared inventory.
     */
    private boolean isItemNameExists(String itemName) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();

            String[] columns = {COLUMN_ITEM_ID};
            String selection = COLUMN_ITEM_NAME + " = ?";
            String[] selectionArgs = {itemName.trim()};

            cursor = db.query(TABLE_INVENTORY, columns, selection, selectionArgs, null, null, null);

            boolean exists = cursor != null && cursor.getCount() > 0;
            Log.d(TAG, "Item name '" + itemName + "' exists: " + exists);
            return exists;

        } catch (Exception e) {
            Log.e(TAG, "Error checking item name existence: " + e.getMessage(), e);
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Retrieves ALL inventory items.
     */
    public List<InventoryActivity.InventoryItem> getAllInventoryItems() {
        List<InventoryActivity.InventoryItem> items = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();

            // Query all items with last editor information for display.
            String query = "SELECT i." + COLUMN_ITEM_NAME + ", i." + COLUMN_ITEM_QUANTITY +
                    ", i." + COLUMN_ITEM_PRICE + ", u." + COLUMN_USERNAME + " as last_editor " +
                    "FROM " + TABLE_INVENTORY + " i " +
                    "LEFT JOIN " + TABLE_USERS + " u ON i." + COLUMN_ITEM_LAST_MODIFIED_BY + " = u." + COLUMN_USER_ID +
                    " ORDER BY i." + COLUMN_ITEM_MODIFIED_DATE + " DESC";

            cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_NAME));
                    int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_QUANTITY));
                    double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ITEM_PRICE));
                    String lastEditor = cursor.getString(cursor.getColumnIndexOrThrow("last_editor"));

                    InventoryActivity.InventoryItem item = new InventoryActivity.InventoryItem(name, quantity, price);

                    // Store last editor info in the item for display purposes.
                    item.setLastEditedBy(lastEditor != null ? lastEditor : "unknown");
                    items.add(item);

                } while (cursor.moveToNext());
            }

            Log.d(TAG, "Retrieved " + items.size() + " shared inventory items");

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving inventory items: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return items;
    }

    /**
     * Updates an existing inventory item in shared database.
     */
    public int updateInventoryItem(String itemName, int newQuantity, double newPrice, long userId) {
        SQLiteDatabase db = null;

        try {
            db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(COLUMN_ITEM_QUANTITY, newQuantity);
            values.put(COLUMN_ITEM_PRICE, newPrice);
            values.put(COLUMN_ITEM_LAST_MODIFIED_BY, userId);
            values.put(COLUMN_ITEM_MODIFIED_DATE, "datetime('now')");

            String whereClause = COLUMN_ITEM_NAME + " = ?";
            String[] whereArgs = {itemName};

            int rowsAffected = db.update(TABLE_INVENTORY, values, whereClause, whereArgs);

            if (rowsAffected > 0) {
                Log.d(TAG, "Inventory item updated successfully: " + itemName);
            } else {
                Log.d(TAG, "No inventory item found to update: " + itemName);
            }

            return rowsAffected;

        } catch (Exception e) {
            Log.e(TAG, "Error updating inventory item: " + e.getMessage(), e);
            return 0;
        } finally {
            if (db != null) db.close();
        }
    }

    /**
     * Deletes an inventory item from the shared database.
     */
    public int deleteInventoryItem(String itemName) {
        SQLiteDatabase db = null;

        try {
            db = this.getWritableDatabase();

            String whereClause = COLUMN_ITEM_NAME + " = ?";
            String[] whereArgs = {itemName};

            int rowsAffected = db.delete(TABLE_INVENTORY, whereClause, whereArgs);

            if (rowsAffected > 0) {
                Log.d(TAG, "Inventory item deleted successfully: " + itemName);
            } else {
                Log.d(TAG, "No inventory item found to delete: " + itemName);
            }

            return rowsAffected;

        } catch (Exception e) {
            Log.e(TAG, "Error deleting inventory item: " + e.getMessage(), e);
            return 0;
        } finally {
            if (db != null) db.close();
        }
    }

    /**
     * Hashes a password using SHA-256.
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
            Log.e(TAG, "Error hashing password: " + e.getMessage(), e);
            return password; // Fallback (not recommended for production)
        }
    }

    /**
     * Clear all users.
     */
    public void clearAllUsers() {
        SQLiteDatabase db = null;

        try {
            db = this.getWritableDatabase();

            // Delete all users.
            int deletedRows = db.delete(TABLE_USERS, null, null);

            Log.d(TAG, "Cleared " + deletedRows + " users from database");

            // Recreate default user.
            insertDefaultUser(db);

        } catch (Exception e) {
            Log.e(TAG, "Error clearing users: " + e.getMessage(), e);
        } finally {
            if (db != null) db.close();
        }
    }

    /**
     * Clear all inventory items.
     */
    public void clearAllInventory() {
        SQLiteDatabase db = null;

        try {
            db = this.getWritableDatabase();

            int deletedRows = db.delete(TABLE_INVENTORY, null, null);

            Log.d(TAG, "Cleared " + deletedRows + " inventory items from database");

        } catch (Exception e) {
            Log.e(TAG, "Error clearing inventory: " + e.getMessage(), e);
        } finally {
            if (db != null) db.close();
        }
    }
}