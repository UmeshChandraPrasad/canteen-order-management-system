package com.canteen.model;

/** Unit I — Inheritance: an Admin IS-A User with the ADMIN role. */
public class Admin extends User {

    public Admin() {
        super();
        setRole(Role.ADMIN);
    }

    public Admin(int id, String fullName, String email,
                 String passwordHash, boolean active) {
        super(id, fullName, email, passwordHash, Role.ADMIN, active);
    }
}
