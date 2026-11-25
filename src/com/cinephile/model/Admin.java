package com.cinephile.model;

/**
 * Admin subclass demonstrating Inheritance, super(), and Method Overriding.
 */
public class Admin extends UserAccount {

    public Admin(long id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public String getRoleName() {
        return "ADMIN";
    }

    @Override
    public String getPermissionsSummary() {
        return "Manage Movies, Theatres, Screens, Showtimes, Bookings & Revenue Reports";
    }
}
