package com.canteen.model;

/** Outcome of one payment attempt. */
public class PaymentResult {
    private final boolean success;
    private final String transactionRef;
    private final String message;

    public PaymentResult(boolean success, String transactionRef, String message) {
        this.success = success;
        this.transactionRef = transactionRef;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public String getTransactionRef() { return transactionRef; }
    public String getMessage() { return message; }
}
