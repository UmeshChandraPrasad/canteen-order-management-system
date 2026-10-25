package com.canteen.service;

import com.canteen.dao.UserDAO;
import com.canteen.dao.impl.JdbcUserDao;
import com.canteen.model.Customer;
import com.canteen.model.Role;
import com.canteen.model.User;
import com.canteen.util.Passwords;

import java.sql.SQLException;

/**
 * Registration and login. All validation failures raise
 * IllegalArgumentException with a *friendly* message — the UI shows
 * exactly this text in a dialog, never a stack trace.
 */
public class AuthService {
    private final UserDAO users = new JdbcUserDao();

    public Customer register(String fullName, String email, String password,
                             String confirmPassword) throws SQLException {
        fullName = fullName == null ? "" : fullName.trim();
        email = email == null ? "" : email.trim().toLowerCase();

        if (fullName.length() < 2) {
            throw new IllegalArgumentException(
                    "Please enter your full name.");
        }
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException(
                    "Please enter a valid email address.");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException(
                    "Password must be at least 4 characters long.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException(
                    "Passwords do not match. Please retype them.");
        }
        if (users.findByEmail(email) != null) {
            throw new IllegalArgumentException(
                    "This email is already registered. Please log in instead.");
        }

        Customer c = new Customer();
        c.setFullName(fullName);
        c.setEmail(email);
        c.setPasswordHash(Passwords.sha256(password));
        c.setRole(Role.CUSTOMER);
        c.setActive(true);
        c.setId(users.insert(c));
        return c;
    }

    public User login(String email, String password) throws SQLException {
        email = email == null ? "" : email.trim().toLowerCase();
        if (email.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please enter both email and password.");
        }
        User user = users.findByEmail(email);
        if (user == null || !Passwords.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "Invalid email or password. Please try again.");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException(
                    "This account has been deactivated. Please contact the canteen.");
        }
        return user;
    }
}
