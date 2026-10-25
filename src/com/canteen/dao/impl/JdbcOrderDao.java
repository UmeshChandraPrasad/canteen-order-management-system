package com.canteen.dao.impl;

import com.canteen.dao.OrderDAO;
import com.canteen.model.Order;
import com.canteen.model.OrderItem;
import com.canteen.model.OrderStatus;
import com.canteen.model.PaymentMethod;
import com.canteen.util.Db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit V — JDBC implementation for orders. Writes that must be atomic
 * receive the caller's transaction Connection; reads open their own.
 */
public class JdbcOrderDao implements OrderDAO {

    private static final String ORDER_COLS =
            "o.id, o.user_id, u.full_name AS customer_name, o.status,"
            + " o.payment_method, o.subtotal, o.charges, o.total,"
            + " o.pickup_notes, o.idempotency_key, o.created_at";

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setUserId(rs.getInt("user_id"));
        o.setCustomerName(rs.getString("customer_name"));
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        o.setSubtotal(rs.getDouble("subtotal"));
        o.setCharges(rs.getDouble("charges"));
        o.setTotal(rs.getDouble("total"));
        o.setPickupNotes(rs.getString("pickup_notes"));
        o.setIdempotencyKey(rs.getString("idempotency_key"));
        Timestamp ts = rs.getTimestamp("created_at");
        o.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
        return o;
    }

    private OrderItem mapItem(ResultSet rs) throws SQLException {
        OrderItem it = new OrderItem();
        it.setId(rs.getInt("id"));
        it.setOrderId(rs.getInt("order_id"));
        it.setMenuItemId(rs.getInt("menu_item_id"));
        it.setItemName(rs.getString("item_name"));
        it.setQuantity(rs.getInt("quantity"));
        it.setUnitPrice(rs.getDouble("unit_price"));
        return it;
    }

    private void loadItems(Connection c, Order order) throws SQLException {
        String sql = "SELECT oi.id, oi.order_id, oi.menu_item_id,"
                + " m.name AS item_name, oi.quantity, oi.unit_price"
                + " FROM order_items oi JOIN menu_items m ON m.id = oi.menu_item_id"
                + " WHERE oi.order_id = ? ORDER BY oi.id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, order.getId());
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapItem(rs));
                }
                order.setItems(items);
            }
        }
    }

    private List<Order> queryOrders(String where, Object... params) throws SQLException {
        String sql = "SELECT " + ORDER_COLS + " FROM orders o"
                + " JOIN users u ON u.id = o.user_id " + where
                + " ORDER BY o.created_at DESC";
        List<Order> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapOrder(rs));
                }
            }
            for (Order o : out) {
                loadItems(c, o);
            }
        }
        return out;
    }

    // ---------------- transactional primitives ----------------

    @Override
    public int insertOrder(Connection tx, Order order) throws SQLException {
        String sql = "INSERT INTO orders (user_id, status, payment_method,"
                + " subtotal, charges, total, pickup_notes, idempotency_key)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = tx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getUserId());
            ps.setString(2, order.getStatus().name());
            ps.setString(3, order.getPaymentMethod().name());
            ps.setDouble(4, order.getSubtotal());
            ps.setDouble(5, order.getCharges());
            ps.setDouble(6, order.getTotal());
            ps.setString(7, order.getPickupNotes());
            ps.setString(8, order.getIdempotencyKey());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Inserting order failed: no id generated.");
    }

    @Override
    public void insertItems(Connection tx, int orderId, List<OrderItem> items)
            throws SQLException {
        String sql = "INSERT INTO order_items (order_id, menu_item_id, quantity, unit_price)"
                + " VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = tx.prepareStatement(sql)) {
            for (OrderItem it : items) {
                ps.setInt(1, orderId);
                ps.setInt(2, it.getMenuItemId());
                ps.setInt(3, it.getQuantity());
                ps.setDouble(4, it.getUnitPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public void insertStatusHistory(Connection tx, int orderId,
                                    OrderStatus from, OrderStatus to, String note)
            throws SQLException {
        String sql = "INSERT INTO order_status_history"
                + " (order_id, from_status, to_status, note) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = tx.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, from == null ? null : from.name());
            ps.setString(3, to.name());
            ps.setString(4, note == null ? "" : note);
            ps.executeUpdate();
        }
    }

    @Override
    public OrderStatus lockStatus(Connection tx, int orderId) throws SQLException {
        String sql = "SELECT status FROM orders WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = tx.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return OrderStatus.valueOf(rs.getString("status"));
                }
            }
        }
        return null;
    }

    @Override
    public void updateStatus(Connection tx, int orderId, OrderStatus to)
            throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (PreparedStatement ps = tx.prepareStatement(sql)) {
            ps.setString(1, to.name());
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    // ---------------- reads ----------------

    @Override
    public Order findById(int id) throws SQLException {
        List<Order> list = queryOrders("WHERE o.id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Order> findByUser(int userId) throws SQLException {
        return queryOrders("WHERE o.user_id = ?", userId);
    }

    @Override
    public List<Order> findActive() throws SQLException {
        return queryOrders("WHERE o.status NOT IN ('COMPLETED','CANCELLED')");
    }

    @Override
    public List<Order> findAll() throws SQLException {
        return queryOrders("");
    }

    @Override
    public List<Order> findCreatedOn(LocalDate day) throws SQLException {
        return queryOrders("WHERE DATE(o.created_at) = ?", Date.valueOf(day));
    }
}
