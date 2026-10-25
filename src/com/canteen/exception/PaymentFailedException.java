package com.canteen.exception;

/** Unit II — Custom exception: the payment step did not succeed. */
@SuppressWarnings("serial")
public class PaymentFailedException extends Exception {
    public PaymentFailedException(String message) {
        super(message);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
