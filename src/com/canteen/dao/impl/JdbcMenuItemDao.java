package com.canteen.dao.impl;

import com.canteen.dao.MenuItemDAO;
import com.canteen.model.MenuItem;
import com.canteen.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit V — JDBC implementation for menu items.
 * Unit II — try-with-resources on every Connection/PreparedStatement/
 * ResultSet so nothing leaks, even on exceptions.
 */
public class JdbcMenuItemDao implements MenuItemDAO {

    private static final String BASE_SELECT =
            "SELECT m.id, m.category_id, c.name AS category_name, m.name,"
            + " m.description, m.price, m.is_veg, m.is_available,"
            + " m.stock_qty, m.image_path"
            + " FROM menu_items m JOIN categories c ON c.id = m.category_id ";

    /** Unit V — ResultSet-to-object mapping. */
    private MenuItem mapRow(ResultSet rs) throws SQLException {
        MenuItem m = new MenuItem();
        m.setId(rs.getInt("id"));
        m.setCategoryId(rs.getInt("category_id"));
        m.setCategoryName(rs.getString("category_name"));
        m.setName(rs.getString("name"));
        m.setDescription(rs.getString("description"));
        m.setPrice(rs.getDouble("price"));
        m.setVeg(rs.getBoolean("is_veg"));
        m.setAvailable(rs.getBoolean("is_available"));
        m.setStockQty(rs.getInt("stock_qty"));
        m.setImagePath(rs.getString("image_path"));
        return m;
    }

    @Override
    public List<MenuItem> findAll() throws SQLException {
        List<MenuItem> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE_SELECT + "ORDER BY c.name, m.name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(mapRow(rs));
            }
        }
        return out;
    }

    @Override
    public List<MenuItem> findAvailable() throws SQLException {
        List<MenuItem> out = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE m.is_available = 1 ORDER BY c.name, m.name";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(mapRow(rs));
            }
        }
        return out;
    }

    @Override
    public List<MenuItem> findByCategory(int categoryId) throws SQLException {
        List<MenuItem> out = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE m.category_id = ? ORDER BY m.name";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        }
        return out;
    }

    @Override
    public MenuItem findById(int id) throws SQLException {
        String sql = BASE_SELECT + "WHERE m.id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public MenuItem findByIdForUpdate(Connection tx, int id) throws SQLException {
        String sql = BASE_SELECT + "WHERE m.id = ? FOR UPDATE";
        try (PreparedStatement ps = tx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public int insert(MenuItem item) throws SQLException {
        String sql = "INSERT INTO menu_items (category_id, name, description, price,"
                + " is_veg, is_available, stock_qty, image_path)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, item);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Inserting menu item failed: no id generated.");
    }

    @Override
    public void update(MenuItem item) throws SQLException {
        String sql = "UPDATE menu_items SET category_id = ?, name = ?, description = ?,"
                + " price = ?, is_veg = ?, is_available = ?, stock_qty = ?,"
                + " image_path = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, item);
            ps.setInt(9, item.getId());
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, MenuItem item) throws SQLException {
        ps.setInt(1, item.getCategoryId());
        ps.setString(2, item.getName());
        ps.setString(3, item.getDescription());
        ps.setDouble(4, item.getPrice());
        ps.setBoolean(5, item.isVeg());
        ps.setBoolean(6, item.isAvailable());
        ps.setInt(7, item.getStockQty());
        ps.setString(8, item.getImagePath());
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM menu_items WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void setAvailable(int id, boolean available) throws SQLException {
        String sql = "UPDATE menu_items SET is_available = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, available);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void decrementStock(Connection tx, int id, int qty) throws SQLException {
        String sql = "UPDATE menu_items SET stock_qty = stock_qty - ?"
                + " WHERE id = ? AND stock_qty >= ?";
        try (PreparedStatement ps = tx.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, id);
            ps.setInt(3, qty);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException(
                        "Insufficient stock for menu item id " + id);
            }
        }
    }
}
