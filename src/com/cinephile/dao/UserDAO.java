package com.cinephile.dao;

import com.cinephile.exception.CinephileException;
import com.cinephile.exception.InvalidCredentialsException;
import com.cinephile.model.Admin;
import com.cinephile.model.Customer;

import java.sql.*;

public class UserDAO {

    public Customer authenticateCustomer(String email, String password)
            throws InvalidCredentialsException, CinephileException {
        String sql = "SELECT customer_id, name, email, phone FROM customers WHERE email = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                            rs.getLong("customer_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("phone")
                    );
                }
            }
            throw new InvalidCredentialsException("Invalid customer email or password.");
        } catch (SQLException e) {
            throw new CinephileException("Database error during customer login: " + e.getMessage(), e);
        }
    }

    public Admin authenticateAdmin(String email, String password)
            throws InvalidCredentialsException, CinephileException {
        String sql = "SELECT admin_id, name, email FROM admins WHERE email = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Admin(
                            rs.getLong("admin_id"),
                            rs.getString("name"),
                            rs.getString("email")
                    );
                }
            }
            throw new InvalidCredentialsException("Invalid admin email or password.");
        } catch (SQLException e) {
            throw new CinephileException("Database error during admin login: " + e.getMessage(), e);
        }
    }

    public Customer registerCustomer(String name, String email, String password, String phone)
            throws CinephileException {
        String checkSql = "SELECT customer_id FROM customers WHERE email = ?";
        String insertSql = "INSERT INTO customers (name, email, password, phone) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, email.trim());
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        throw new CinephileException("An account with this email already exists.");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name.trim());
                ps.setString(2, email.trim());
                ps.setString(3, password);
                ps.setString(4, phone == null ? "" : phone.trim());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return new Customer(keys.getLong(1), name.trim(), email.trim(), phone);
                    }
                }
            }
            throw new CinephileException("Registration failed: could not obtain generated ID.");
        } catch (SQLException e) {
            throw new CinephileException("Database error during registration: " + e.getMessage(), e);
        }
    }
}
