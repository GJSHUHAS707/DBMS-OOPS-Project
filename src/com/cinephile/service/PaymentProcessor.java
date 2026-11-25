package com.cinephile.service;

import com.cinephile.exception.PaymentFailedException;

/**
 * Interface for payment handling.
 * Demonstrates CS23333 Unit III & Expt. 6: Interfaces with default and static methods.
 */
public interface PaymentProcessor {

    boolean processPayment(double amount) throws PaymentFailedException;

    String getMethodName();

    default String formatReceipt(String bookingRef, double amount) {
        return "Payment of Rs." + String.format("%.2f", amount)
                + " via " + getMethodName() + " for Booking " + bookingRef;
    }

    static boolean isValidAmount(double amount) {
        return amount > 0.0;
    }
}
