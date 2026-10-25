package com.canteen.dao;

import com.canteen.model.MenuItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Unit I — DAO interface: persistence contract for menu items. */
public interface MenuItemDAO {
    List<MenuItem> findAll() throws SQLException;
    List<MenuItem> findAvailable() throws SQLException;
    List<MenuItem> findByCategory(int categoryId) throws SQLException;
    MenuItem findById(int id) throws SQLException;

    /**
     * Row-level lock variant used inside the checkout transaction so two
     * checkouts can never oversell the same item.
     */
    MenuItem findByIdForUpdate(Connection tx, int id) throws SQLException;

    int insert(MenuItem item) throws SQLException;
    void update(MenuItem item) throws SQLException;
    void delete(int id) throws SQLException;
    void setAvailable(int id, boolean available) throws SQLException;

    /** Decrease stock inside a transaction; throws if stock would go negative. */
    void decrementStock(Connection tx, int id, int qty) throws SQLException;
}
