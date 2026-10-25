package com.canteen.model;

/** Unit I — Inheritance: a Customer IS-A User with the CUSTOMER role. */
public class Customer extends User {

    public Customer() {
        super();
        setRole(Role.CUSTOMER);
    }

    public Customer(int id, String fullName, String email,
                    String passwordHash, boolean active) {
        super(id, fullName, email, passwordHash, Role.CUSTOMER, active);
    }
}
