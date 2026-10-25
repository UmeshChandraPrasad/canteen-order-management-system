package com.canteen.model;

import java.util.UUID;

/**
 * SIMULATED UPI — for coursework demo only. No real money moves, no
 * gateway is contacted; it just pretends a payment succeeded and hands
 * back a fake transaction reference. Every UI surface that offers UPI
 * must display the text "SIMULATED / DEMO — no real money".
 */
public class SimulatedUpiPayment extends Payment {

    /** The exact label every UPI control must show (kept in one place). */
    public static final String DEMO_LABEL = "SIMULATED / DEMO \u2014 no real money";

    @Override
    public PaymentResult pay(double amount) {
        String ref = "SIM-UPI-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();
        return new PaymentResult(true, ref,
                "Simulated UPI payment of ₹" + String.format("%.2f", amount)
                + " approved (demo only — no real money moved). Ref: " + ref);
    }

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.UPI_SIMULATED;
    }
}
