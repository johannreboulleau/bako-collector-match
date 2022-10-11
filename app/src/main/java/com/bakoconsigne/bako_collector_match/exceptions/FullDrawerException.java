package com.bakoconsigne.bako_collector_match.exceptions;

/**
 * Exception if drawers are full
 */
public class FullDrawerException extends RuntimeException {

    public FullDrawerException() {
    }

    public FullDrawerException(String message) {
        super(message);
    }
}
