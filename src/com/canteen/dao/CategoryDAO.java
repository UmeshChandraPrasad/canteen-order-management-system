package com.canteen.dao;

import com.canteen.model.Category;

import java.sql.SQLException;
import java.util.List;

/** Unit I — DAO interface: persistence contract for categories. */
public interface CategoryDAO {
    List<Category> findAll() throws SQLException;
    Category findById(int id) throws SQLException;
    int insert(String name) throws SQLException;
}
