package com.canteen.model;

import com.canteen.exception.ItemUnavailableException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit III — Collections: the cart is backed by a HashMap keyed by
 * menu-item id; Unit III — Streams: totals are computed with streams.
 */
public class Cart {
    private final Map<Integer, CartLine> lines = new HashMap<>();

    /** Add {@code qty} of {@code item}; merges with an existing line. */
    public void addItem(MenuItem item, int qty) throws ItemUnavailableException {
        if (item == null) {
            throw new ItemUnavailableException("That item no longer exists.");
        }
        if (!item.isAvailable()) {
            throw new ItemUnavailableException(
                    "'" + item.getName() + "' is currently unavailable.");
        }
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        CartLine existing = lines.get(item.getId());
        int newQty = (existing == null ? 0 : existing.getQuantity()) + qty;
        if (newQty > item.getStockQty()) {
            throw new ItemUnavailableException(
                    "Only " + item.getStockQty() + " x '" + item.getName()
                    + "' left in stock.");
        }
        if (existing == null) {
            lines.put(item.getId(), new CartLine(item, qty));
        } else {
            existing.setQuantity(newQty);
        }
    }

    public void updateQuantity(int menuItemId, int qty) throws ItemUnavailableException {
        CartLine line = lines.get(menuItemId);
        if (line == null) {
            return;
        }
        if (qty <= 0) {
            lines.remove(menuItemId);
            return;
        }
        if (qty > line.getItem().getStockQty()) {
            throw new ItemUnavailableException(
                    "Only " + line.getItem().getStockQty() + " x '"
                    + line.getItem().getName() + "' left in stock.");
        }
        line.setQuantity(qty);
    }

    public void removeItem(int menuItemId) {
        lines.remove(menuItemId);
    }

    public void clear() {
        lines.clear();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** Snapshot list for tables and for checkout. */
    public List<CartLine> getLines() {
        return new ArrayList<>(lines.values());
    }

    /** Unit III — Streams: bill total via mapToDouble().sum(). */
    public double getSubtotal() {
        return lines.values().stream()
                .mapToDouble(CartLine::lineTotal)
                .sum();
    }

    public int getTotalQuantity() {
        return lines.values().stream().mapToInt(CartLine::getQuantity).sum();
    }
}
