package com.example.tristenbradneyinventoryapplication;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity for users.
 * Represents the users table with unique username constraint.
 */
@Entity(tableName = "users",
        indices = {@Index(value = "username", unique = true)})
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    private long userId;

    @ColumnInfo(name = "username")
    private String username;

    @ColumnInfo(name = "password_hash")
    private String passwordHash;

    @ColumnInfo(name = "created_date")
    private String createdDate;

    // Constructor
    public UserEntity(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    // Getters and Setters
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }
}