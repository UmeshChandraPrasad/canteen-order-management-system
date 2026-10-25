package com.canteen.exception;

/** Unit II — Custom exception: no order exists with the given id. */
@SuppressWarnings("serial")
public class OrderNotFoundException extends Exception {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
