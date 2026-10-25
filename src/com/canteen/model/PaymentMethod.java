package com.canteen.model;

/**
 * Supported payment methods. UPI is SIMULATED ONLY — no real money
 * moves; every UPI screen carries a visible "SIMULATED / DEMO" label.
 */
public enum PaymentMethod {
    CASH_ON_PICKUP,
    UPI_SIMULATED;

    public String label() {
        return this == CASH_ON_PICKUP ? "Cash on Pickup" : "UPI (Simulated)";
    }
}
