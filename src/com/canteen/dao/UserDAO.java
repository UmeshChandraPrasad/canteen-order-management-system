package com.canteen.dao;

import com.canteen.model.User;

import java.sql.SQLException;
import java.util.List;

/** Unit I — DAO interface: persistence contract for users. */
public interface UserDAO {
    User findByEmail(String email) throws SQLException;
    User findById(int id) throws SQLException;
    List<User> findAll() throws SQLException;
    int insert(User user) throws SQLException;
    void setActive(int id, boolean active) throws SQLException;
}
