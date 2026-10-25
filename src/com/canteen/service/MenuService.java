package com.canteen.service;

import com.canteen.dao.CategoryDAO;
import com.canteen.dao.MenuItemDAO;
import com.canteen.dao.impl.JdbcCategoryDao;
import com.canteen.dao.impl.JdbcMenuItemDao;
import com.canteen.model.Category;
import com.canteen.model.MenuItem;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Menu read/write operations. Filtering and sorting are done with
 * Java Streams (Unit III) over the DAO results.
 */
public class MenuService {
    private final MenuItemDAO items = new JdbcMenuItemDao();
    private final CategoryDAO categories = new JdbcCategoryDao();

    public enum SortOrder {
        NAME_ASC("Name (A–Z)", Comparator.comparing(MenuItem::getName,
                String.CASE_INSENSITIVE_ORDER)),
        PRICE_ASC("Price (low → high)", Comparator.comparingDouble(MenuItem::getPrice)),
        PRICE_DESC("Price (high → low)",
                Comparator.comparingDouble(MenuItem::getPrice).reversed());

        private final String label;
        private final Comparator<MenuItem> comparator;

        SortOrder(String label, Comparator<MenuItem> comparator) {
            this.label = label;
            this.comparator = comparator;
        }

        public Comparator<MenuItem> comparator() { return comparator; }

        @Override
        public String toString() { return label; }
    }

    public List<Category> listCategories() throws SQLException {
        return categories.findAll();
    }

    /** Full menu for the admin screen (includes unavailable items). */
    public List<MenuItem> listAllItems() throws SQLException {
        return items.findAll();
    }

    /**
     * Unit III — Streams: customer-facing menu with optional veg-only
     * filter, category filter, text search and sorting — all stream
     * operations over one DAO fetch.
     */
    public List<MenuItem> browseMenu(String search, Integer categoryId,
                                     boolean vegOnly, SortOrder sort)
            throws SQLException {
        String q = search == null ? "" : search.trim().toLowerCase();
        return items.findAvailable().stream()
                .filter(m -> !vegOnly || m.isVeg())                       // veg-only filter
                .filter(m -> categoryId == null || m.getCategoryId() == categoryId)
                .filter(m -> q.isEmpty()
                        || m.getName().toLowerCase().contains(q)
                        || m.getDescription().toLowerCase().contains(q))
                .sorted(sort == null ? SortOrder.NAME_ASC.comparator()
                                     : sort.comparator())                // sort
                .collect(Collectors.toList());
    }

    public MenuItem saveItem(MenuItem item) throws SQLException {
        validate(item);
        if (item.getId() == 0) {
            item.setId(items.insert(item));
        } else {
            items.update(item);
        }
        return item;
    }

    public void deleteItem(int id) throws SQLException {
        try {
            items.delete(id);
        } catch (SQLException e) {
            // FK violation -> item is referenced by old orders
            throw new SQLException(
                    "Cannot delete this item because it appears in past orders. "
                    + "Mark it unavailable instead.", e);
        }
    }

    public void toggleAvailability(int id, boolean available) throws SQLException {
        items.setAvailable(id, available);
    }

    private void validate(MenuItem item) {
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty.");
        }
        if (item.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }
        if (item.getStockQty() < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }
    }
}
