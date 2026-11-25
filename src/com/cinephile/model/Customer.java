package com.cinephile.model;

/**
 * Customer subclass demonstrating Inheritance, super(), and Method Overriding.
 */
public class Customer extends UserAccount {
    private String phone;

    public Customer(long id, String name, String email, String phone) {
        super(id, name, email);
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String getRoleName() {
        return "CUSTOMER";
    }

    @Override
    public String getPermissionsSummary() {
        return "Browse Movies, Select Seats, Book & Cancel Tickets";
    }
}
