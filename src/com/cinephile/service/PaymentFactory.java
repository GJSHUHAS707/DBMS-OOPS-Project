package com.cinephile.service;

public class PaymentFactory {

    public static PaymentProcessor getProcessor(String method) {
        if ("CARD".equalsIgnoreCase(method)) {
            return new CardPaymentProcessor();
        } else if ("CASH".equalsIgnoreCase(method)) {
            return new CashPaymentProcessor();
        }
        return new UpiPaymentProcessor();
    }
}
