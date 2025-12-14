package com.example.tristenbradneyinventoryapplication.utils;

import com.example.tristenbradneyinventoryapplication.InventoryItemEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class LowStockManager {

    /**
     * Priority queue storing low-stock items ordered by urgency.
     * The comparator ensures items with the lowest (quantity/threshold) ratio appear first.
     */

    private PriorityQueue<InventoryItemEntity> priorityQueue;

    /**
     * Default threshold for considering an item as "low stock".
     * Items with quantity <= this value will be flagged.
     */

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    /**
     * Constructs a new LowStockManager with a min-heap based on urgency ratio.
     *
     * Urgency Calculation: (current quantity / low stock threshold)
     * - Lower ratio = more urgent
     * - Ratio of 0 means completely out of stock
     * - Ratio < 1 means below threshold
     */

    public LowStockManager() {
        // Create min-heap based on urgency ratio
        priorityQueue = new PriorityQueue<>(new Comparator<InventoryItemEntity>() {
            @Override
            public int compare(InventoryItemEntity a, InventoryItemEntity b) {
                // Calculate urgency ratios
                double ratioA = calculateUrgencyRatio(a);
                double ratioB = calculateUrgencyRatio(b);

                // Lower ratio = higher priority (min-heap)
                return Double.compare(ratioA, ratioB);
            }
        });
    }

    /**
     * Calculates the urgency ratio for an inventory item.
     *
     * Formula: current_quantity / threshold
     *
     * Examples:
     * - Item with 0 quantity: ratio = 0.0 (most urgent)
     * - Item with 5 quantity, threshold 10: ratio = 0.5
     * - Item with 10 quantity, threshold 10: ratio = 1.0 (at threshold)
     * - Item with 15 quantity, threshold 10: ratio = 1.5 (above threshold)
     *
     * @param item The inventory item to evaluate
     * @return The urgency ratio (lower = more urgent)
     */

    private double calculateUrgencyRatio(InventoryItemEntity item) {
        int threshold = DEFAULT_LOW_STOCK_THRESHOLD;

        // Calculate ratio (prevents division by zero)
        return (double) item.getQuantity() / threshold;
    }

    /**
     * Updates the priority queue with current low-stock items.
     *
     * Time Complexity: O(m log m) where m is the number of low-stock items
     * - Iterating through all n items: O(n)
     * - Inserting m low-stock items into heap: O(m log m)
     * - Total: O(n + m log m)
     *
     * @param allItems The complete inventory list to scan
     */

    public void updateLowStockItems(List<InventoryItemEntity> allItems) {
        // Clear existing priority queue
        priorityQueue.clear();

        // Scan all items and add low-stock items to priority queue
        for (InventoryItemEntity item : allItems) {
            int threshold = DEFAULT_LOW_STOCK_THRESHOLD;

            // Add to priority queue if below or at threshold
            if (item.getQuantity() <= threshold) {
                priorityQueue.offer(item);  // O(log n) insertion
            }
        }
    }

    /**
     * Retrieves all low-stock items in priority order (most urgent first).
     *
     * Time Complexity: O(m) where m is the number of low-stock items
     * Space Complexity: O(m) for the returned list
     *
     * Note: This creates a copy to preserve the heap structure.
     *
     * @return List of low-stock items sorted by urgency
     */

    public List<InventoryItemEntity> getLowStockItemsSorted() {
        // Create a list from the priority queue (maintains heap order)
        return new ArrayList<>(priorityQueue);
    }

    /**
     * Gets the most urgent low-stock item without removing it from the queue.
     *
     * Time Complexity: O(1) - constant time operation
     *
     * @return The most urgent item, or null if no low-stock items exist
     */

    public InventoryItemEntity getMostUrgentItem() {
        return priorityQueue.peek();  // O(1) operation
    }

    /**
     * Removes and returns the most urgent low-stock item.
     *
     * Time Complexity: O(log n) for heap restructuring
     *
     * @return The most urgent item, or null if queue is empty
     */

    public InventoryItemEntity removeMostUrgentItem() {
        return priorityQueue.poll();  // O(log n) operation
    }

    /**
     * Checks if there are any low-stock items.
     *
     * Time Complexity: O(1)
     *
     * @return True if there are low-stock items, false otherwise
     */

    public boolean hasLowStockItems() {
        return !priorityQueue.isEmpty();
    }

    /**
     * Gets the count of low-stock items.
     *
     * Time Complexity: O(1)
     *
     * @return The number of items currently flagged as low stock
     */

    public int getLowStockCount() {
        return priorityQueue.size();
    }

    /**
     * Gets items that are critically low (quantity is 0 or very close to 0).
     *
     * Time Complexity: O(m) where m is the number of low-stock items
     *
     * @return List of critically low items
     */

    public List<InventoryItemEntity> getCriticalItems() {
        List<InventoryItemEntity> criticalItems = new ArrayList<>();

        for (InventoryItemEntity item : priorityQueue) {
            // Consider critical if urgency ratio is less than 0.25
            // (less than 25% of threshold)
            if (calculateUrgencyRatio(item) < 0.25) {
                criticalItems.add(item);
            }
        }

        return criticalItems;
    }

    /**
     * Gets a status summary of low-stock items by urgency level.
     *
     * @return LowStockStatus object containing categorized counts
     */

    public LowStockStatus getStatus() {
        int critical = 0;   // Ratio < 0.25
        int warning = 0;    // 0.25 <= Ratio < 0.75
        int lowStock = 0;   // 0.75 <= Ratio <= 1.0

        for (InventoryItemEntity item : priorityQueue) {
            double ratio = calculateUrgencyRatio(item);

            if (ratio < 0.25) {
                critical++;
            } else if (ratio < 0.75) {
                warning++;
            } else {
                lowStock++;
            }
        }

        return new LowStockStatus(critical, warning, lowStock);
    }

    /**
     * Inner class to hold low-stock status information.
     */

    public static class LowStockStatus {
        public final int criticalCount;
        public final int warningCount;
        public final int lowStockCount;
        public final int totalCount;

        public LowStockStatus(int critical, int warning, int lowStock) {
            this.criticalCount = critical;
            this.warningCount = warning;
            this.lowStockCount = lowStock;
            this.totalCount = critical + warning + lowStock;
        }

        @Override
        public String toString() {
            return String.format("Critical: %d, Warning: %d, Low Stock: %d, Total: %d",
                    criticalCount, warningCount, lowStockCount, totalCount);
        }
    }
}
