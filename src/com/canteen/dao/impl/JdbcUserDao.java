package com.canteen.dao.impl;

import com.canteen.dao.UserDAO;
import com.canteen.model.Admin;
import com.canteen.model.Customer;
import com.canteen.model.Role;
import com.canteen.model.User;
import com.canteen.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit V — JDBC: every query is a PreparedStatement (never string
 * concatenation); ResultSet rows are mapped to User objects here.
 */
public class JdbcUserDao implements UserDAO {

    private static final String COLS =
            "id, full_name, email, password_hash, role, is_active";

    /** Unit V — ResultSet-to-object mapping. */
    private User mapRow(ResultSet rs) throws SQLException {
        Role role = Role.valueOf(rs.getString("role"));
        User user = (role == Role.ADMIN) ? new Admin() : new Customer();
        user.setId(rs.getInt("id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(role);
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }

    @Override
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT " + COLS + " FROM users WHERE email = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public User findById(int id) throws SQLException {
        String sql = "SELECT " + COLS + " FROM users WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public List<User> findAll() throws SQLException {
        String sql = "SELECT " + COLS + " FROM users ORDER BY full_name";
        List<User> out = new ArrayList<>();
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
    public int insert(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, password_hash, role, is_active)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Inserting user failed: no id generated.");
    }

    @Override
    public void setActive(int id, boolean active) throws SQLException {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
