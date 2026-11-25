package com.cinephile.model;

/**
 * Abstract base class representing any authenticated user in Cinephile.
 * Demonstrates CS23333 Unit I & II: Encapsulation, Abstraction, Constructors, Inheritance.
 */
public abstract class UserAccount {
    private long id;
    private String name;
    private String email;

    protected UserAccount(long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Abstract methods to be overridden by Customer and Admin subclasses
    public abstract String getRoleName();

    public abstract String getPermissionsSummary();

    @Override
    public String toString() {
        return name + " (" + getRoleName() + " - " + email + ")";
    }
}
