package com.canteen.model;

import com.canteen.exception.PaymentFailedException;

/**
 * Unit I — Abstraction + Polymorphism: Payment is abstract; concrete
 * subclasses implement pay() their own way and the checkout code only
 * talks to this type, never to the concrete class.
 */
public abstract class Payment {

    /** Charge {@code amount} (in rupees). Never returns null. */
    public abstract PaymentResult pay(double amount) throws PaymentFailedException;

    /** Which PaymentMethod enum value this payment maps to. */
    public abstract PaymentMethod getMethod();
}
