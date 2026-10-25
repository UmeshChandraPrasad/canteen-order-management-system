package com.canteen.model;

/**
 * Unit I — Inheritance: User is the superclass; Customer and Admin
 * extend it and fix their role. All fields are private (encapsulation).
 */
public class User {
    private int id;
    private String fullName;
    private String email;
    private String passwordHash; // SHA-256 hex, never the plain password
    private Role role;
    private boolean active;

    public User() {
    }

    public User(int id, String fullName, String email, String passwordHash,
                Role role, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isAdmin() { return role == Role.ADMIN; }

    @Override
    public String toString() {
        return fullName + " <" + email + "> [" + role + "]";
    }
}
