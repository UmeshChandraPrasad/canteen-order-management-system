package com.canteen.exception;

/** Unit II — Custom exception: item is off-menu, hidden, or out of stock. */
@SuppressWarnings("serial")
public class ItemUnavailableException extends Exception {
    public ItemUnavailableException(String message) {
        super(message);
    }

    public ItemUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
