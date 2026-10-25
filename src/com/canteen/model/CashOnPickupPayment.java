package com.canteen.model;

/**
 * Cash on Pickup — fully working: the order is recorded with payment
 * due at the pickup counter; nothing is charged online.
 */
public class CashOnPickupPayment extends Payment {

    @Override
    public PaymentResult pay(double amount) {
        String ref = "CASH-" + System.currentTimeMillis();
        return new PaymentResult(true, ref,
                "Please pay ₹" + String.format("%.2f", amount)
                + " in cash at the pickup counter.");
    }

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.CASH_ON_PICKUP;
    }
}
