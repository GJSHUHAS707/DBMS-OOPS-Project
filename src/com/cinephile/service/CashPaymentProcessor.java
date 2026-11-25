package com.cinephile.service;

import com.cinephile.exception.PaymentFailedException;

public class CashPaymentProcessor implements PaymentProcessor {

    @Override
    public boolean processPayment(double amount) throws PaymentFailedException {
        if (!PaymentProcessor.isValidAmount(amount)) {
            throw new PaymentFailedException("Invalid Counter Cash amount: " + amount);
        }
        return true;
    }

    @Override
    public String getMethodName() {
        return "CASH";
    }
}
