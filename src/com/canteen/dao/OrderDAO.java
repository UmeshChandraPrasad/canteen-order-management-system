package com.canteen.dao;

import com.canteen.model.Order;
import com.canteen.model.OrderItem;
import com.canteen.model.OrderStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Unit I — DAO interface: persistence contract for orders. */
public interface OrderDAO {

    // ---- transactional primitives (caller owns the Connection) ----
    int insertOrder(Connection tx, Order order) throws SQLException;
    void insertItems(Connection tx, int orderId, List<OrderItem> items) throws SQLException;
    void insertStatusHistory(Connection tx, int orderId,
                             OrderStatus from, OrderStatus to, String note) throws SQLException;
    OrderStatus lockStatus(Connection tx, int orderId) throws SQLException;
    void updateStatus(Connection tx, int orderId, OrderStatus to) throws SQLException;

    // ---- read convenience (own connection each) ----
    Order findById(int id) throws SQLException;
    List<Order> findByUser(int userId) throws SQLException;
    List<Order> findActive() throws SQLException; // not COMPLETED/CANCELLED
    List<Order> findAll() throws SQLException;
    List<Order> findCreatedOn(LocalDate day) throws SQLException;
}
