package com.example.tristenbradneyinventoryapplication;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

/**
 * Data Access Object for user operations.
 */
@Dao
public interface UserDao {

    /**
     * Insert a new user.
     * @return The row ID of the newly created user
     */
    @Insert
    long insertUser(UserEntity user);

    /**
     * Authenticate a user by username and password hash.
     */
    @Query("SELECT * FROM users WHERE username = :username AND password_hash = :passwordHash LIMIT 1")
    UserEntity authenticateUser(String username, String passwordHash);

    /**
     * Check if username exists.
     */
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    int usernameExists(String username);

    /**
     * Get user by ID.
     */
    @Query("SELECT * FROM users WHERE user_id = :userId")
    UserEntity getUserById(long userId);

    /**
     * Delete all users.
     */
    @Query("DELETE FROM users")
    void deleteAllUsers();
}