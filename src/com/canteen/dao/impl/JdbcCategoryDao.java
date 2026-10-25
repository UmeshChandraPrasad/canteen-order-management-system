package com.canteen.dao.impl;

import com.canteen.dao.CategoryDAO;
import com.canteen.model.Category;
import com.canteen.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Unit V — JDBC implementation for categories. */
public class JdbcCategoryDao implements CategoryDAO {

    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(rs.getInt("id"), rs.getString("name"));
    }

    @Override
    public List<Category> findAll() throws SQLException {
        String sql = "SELECT id, name FROM categories ORDER BY name";
        List<Category> out = new ArrayList<>();
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
    public Category findById(int id) throws SQLException {
        String sql = "SELECT id, name FROM categories WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public int insert(String name) throws SQLException {
        String sql = "INSERT INTO categories (name) VALUES (?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Inserting category failed: no id generated.");
    }
}
