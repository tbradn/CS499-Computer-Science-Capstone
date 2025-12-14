package com.example.tristenbradneyinventoryapplication.utils;

import com.example.tristenbradneyinventoryapplication.InventoryItemEntity;
import java.util.ArrayList;
import java.util.List;

public class InventorySearchManager {

    /**
     * Searches inventory items by name using case-insensitive substring matching.
     *
     * Algorithm: Linear search with string containment check
     * Time Complexity: O(n) where n is the number of items
     * Space Complexity: O(m) where m is the number of matching items
     *
     * @param inventoryList The complete list of inventory items to search through
     * @param query The search term to match against item names
     * @return A filtered list containing only items whose names contain the query string
     */
    public List<InventoryItemEntity> searchByName(List<InventoryItemEntity> inventoryList, String query) {
        List<InventoryItemEntity> results = new ArrayList<>();

        // Handle null or empty query - return all items
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(inventoryList);
        }

        // Convert query to lowercase for case-insensitive comparison
        String queryLower = query.toLowerCase().trim();

        // Linear search through all items - O(n)
        for (InventoryItemEntity item : inventoryList) {
            if (item.getItemName() != null &&
                    item.getItemName().toLowerCase().contains(queryLower)) {
                results.add(item);
            }
        }

        return results;
    }

    /**
     * Performs fuzzy search to find items with similar names.
     * Uses Levenshtein distance for approximate string matching.
     *
     * @param inventoryList The complete list of inventory items
     * @param query The search term
     * @param maxDistance Maximum edit distance to consider a match
     * @return List of items within the specified edit distance
     */
    public List<InventoryItemEntity> fuzzySearch(List<InventoryItemEntity> inventoryList,
                                                 String query, int maxDistance) {
        List<InventoryItemEntity> results = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(inventoryList);
        }

        String queryLower = query.toLowerCase().trim();

        for (InventoryItemEntity item : inventoryList) {
            if (item.getItemName() != null) {
                int distance = calculateLevenshteinDistance(
                        item.getItemName().toLowerCase(),
                        queryLower
                );
                if (distance <= maxDistance) {
                    results.add(item);
                }
            }
        }

        return results;
    }

    /**
     * Calculates Levenshtein distance between two strings.
     * This is the minimum number of single-character edits needed to change one string into another.
     *
     * Time Complexity: O(m * n) where m and n are the lengths of the two strings
     * Space Complexity: O(m * n) for the dynamic programming table
     *
     * @param s1 First string
     * @param s2 Second string
     * @return The edit distance between the two strings
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        // Initialize base cases
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        // Fill the dynamic programming table
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(
                            dp[i - 1][j],      // deletion
                            Math.min(
                                    dp[i][j - 1],  // insertion
                                    dp[i - 1][j - 1] // substitution
                            )
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }
}