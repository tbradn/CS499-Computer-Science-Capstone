package com.example.tristenbradneyinventoryapplication.utils;

import com.example.tristenbradneyinventoryapplication.InventoryItemEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class InventorySorter {

    /**
     * Enum defining available sort modes for inventory items.
     */
    public enum SortMode {

        // Sort by name, A to Z
        NAME_ASC,

        // Sort by name, Z to A
        NAME_DESC,

        // Sort by quantity, low to high
        QUANTITY_ASC,

        // Sort by quantity, high to low
        QUANTITY_DESC,

        // Sort by price, low to high
        PRICE_ASC,

        // Sort by price, high to low
        PRICE_DESC
    }

    /**
     * Sorts inventory items by name in specified order.
     *
     * Algorithm: Uses Java's Collections.sort() which implements TimSort
     * Time Complexity: O(n log n) average and worst case
     * Space Complexity: O(n) for the new list
     * Stability: Stable sort (maintains relative order of equal elements)
     *
     * @param items The list of items to sort
     * @param ascending True for A-Z, false for Z-A
     * @return A new sorted list (original list unchanged)
     */
    public List<InventoryItemEntity> sortByName(List<InventoryItemEntity> items, boolean ascending) {
        // Create a copy to avoid modifying original list
        List<InventoryItemEntity> sorted = new ArrayList<>(items);

        if (ascending) {
            // Sort alphabetically A to Z (case-insensitive)
            Collections.sort(sorted, new Comparator<InventoryItemEntity>() {
                @Override
                public int compare(InventoryItemEntity a, InventoryItemEntity b) {
                    return a.getItemName().compareToIgnoreCase(b.getItemName());
                }
            });
        } else {
            // Sort alphabetically Z to A (case-insensitive)
            Collections.sort(sorted, new Comparator<InventoryItemEntity>() {
                @Override
                public int compare(InventoryItemEntity a, InventoryItemEntity b) {
                    return b.getItemName().compareToIgnoreCase(a.getItemName());
                }
            });
        }

        return sorted;
    }

    /**
     * Sorts inventory items by quantity in specified order.
     *
     * Algorithm: Uses Java's Collections.sort() with integer comparison
     * Time Complexity: O(n log n)
     * Space Complexity: O(n)
     *
     * @param items The list of items to sort
     * @param ascending True for low to high, false for high to low
     * @return A new sorted list
     */
    public List<InventoryItemEntity> sortByQuantity(List<InventoryItemEntity> items, boolean ascending) {
        List<InventoryItemEntity> sorted = new ArrayList<>(items);

        if (ascending) {
            // Sort by quantity: lowest first
            Collections.sort(sorted, new Comparator<InventoryItemEntity>() {
                @Override
                public int compare(InventoryItemEntity a, InventoryItemEntity b) {
                    return Integer.compare(a.getQuantity(), b.getQuantity());
                }
            });
        } else {
            // Sort by quantity: highest first
            Collections.sort(sorted, new Comparator<InventoryItemEntity>() {
                @Override
                public int compare(InventoryItemEntity a, InventoryItemEntity b) {
                    return Integer.compare(b.getQuantity(), a.getQuantity());
                }
            });
        }

        return sorted;
    }

    /**
     * Sorts inventory items by price in specified order.
     *
     * Algorithm: Uses Java's Collections.sort() with double comparison
     * Time Complexity: O(n log n)
     * Space Complexity: O(n)
     *
     * @param items The list of items to sort
     * @param ascending True for low to high, false for high to low
     * @return A new sorted list
     */
    public List<InventoryItemEntity> sortByPrice(List<InventoryItemEntity> items, boolean ascending) {
        List<InventoryItemEntity> sorted = new ArrayList<>(items);

        if (ascending) {
            Collections.sort(sorted, new Comparator<InventoryItemEntity>() {
                @Override
                public int compare(InventoryItemEntity a, InventoryItemEntity b) {
                    return Double.compare(a.getPrice(), b.getPrice());
                }
            });
        } else {
            Collections.sort(sorted, new Comparator<InventoryItemEntity>() {
                @Override
                public int compare(InventoryItemEntity a, InventoryItemEntity b) {
                    return Double.compare(b.getPrice(), a.getPrice());
                }
            });
        }

        return sorted;
    }

    /**
     * Applies the specified sort mode to the list of items.
     *
     * @param items The list of items to sort
     * @param sortMode The sorting mode to apply
     * @return A new sorted list based on the specified mode
     */
    public List<InventoryItemEntity> sort(List<InventoryItemEntity> items, SortMode sortMode) {
        switch (sortMode) {
            case NAME_ASC:
                return sortByName(items, true);
            case NAME_DESC:
                return sortByName(items, false);
            case QUANTITY_ASC:
                return sortByQuantity(items, true);
            case QUANTITY_DESC:
                return sortByQuantity(items, false);
            case PRICE_ASC:
                return sortByPrice(items, true);
            case PRICE_DESC:
                return sortByPrice(items, false);
            default:
                // Default to name ascending
                return sortByName(items, true);
        }
    }

    /**
     * Performs a multi-level sort: primary by one field, secondary by another.
     * For example, sort by quantity first, then by name for items with equal quantity.
     *
     * Time Complexity: O(n log n)
     *
     * @param items The list of items to sort
     * @param primarySort Primary sort criterion
     * @param secondarySort Secondary sort criterion (for tie-breaking)
     * @return A new sorted list
     */
    public List<InventoryItemEntity> multiLevelSort(List<InventoryItemEntity> items,
                                                    SortMode primarySort,
                                                    SortMode secondarySort) {
        List<InventoryItemEntity> sorted = new ArrayList<>(items);

        Collections.sort(sorted, new Comparator<InventoryItemEntity>() {
            @Override
            public int compare(InventoryItemEntity a, InventoryItemEntity b) {
                // Apply primary comparison
                int primaryComparison = compareItems(a, b, primarySort);

                // If equal, use secondary comparison
                if (primaryComparison == 0) {
                    return compareItems(a, b, secondarySort);
                }

                return primaryComparison;
            }
        });

        return sorted;
    }

    /**
     * Helper method to compare two items based on a sort mode.
     *
     * @param a First item
     * @param b Second item
     * @param mode The sort mode to apply
     * @return Negative if a < b, positive if a > b, zero if equal
     */
    private int compareItems(InventoryItemEntity a, InventoryItemEntity b, SortMode mode) {
        switch (mode) {
            case NAME_ASC:
                return a.getItemName().compareToIgnoreCase(b.getItemName());
            case NAME_DESC:
                return b.getItemName().compareToIgnoreCase(a.getItemName());
            case QUANTITY_ASC:
                return Integer.compare(a.getQuantity(), b.getQuantity());
            case QUANTITY_DESC:
                return Integer.compare(b.getQuantity(), a.getQuantity());
            case PRICE_ASC:
                return Double.compare(a.getPrice(), b.getPrice());
            case PRICE_DESC:
                return Double.compare(b.getPrice(), a.getPrice());
            default:
                return 0;
        }
    }
}