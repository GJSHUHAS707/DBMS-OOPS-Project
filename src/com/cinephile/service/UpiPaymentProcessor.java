package com.cinephile.service;

import com.cinephile.exception.PaymentFailedException;

public class UpiPaymentProcessor implements PaymentProcessor {

    @Override
    public boolean processPayment(double amount) throws PaymentFailedException {
        if (!PaymentProcessor.isValidAmount(amount)) {
            throw new PaymentFailedException("Invalid UPI payment amount: " + amount);
        }
        return true;
    }

    @Override
    public String getMethodName() {
        return "UPI";
    }
}
