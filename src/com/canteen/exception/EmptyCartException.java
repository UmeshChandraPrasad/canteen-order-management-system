package com.canteen.exception;

/** Unit II — Custom exception: checkout attempted with an empty cart. */
@SuppressWarnings("serial")
public class EmptyCartException extends Exception {
    public EmptyCartException(String message) {
        super(message);
    }
}
