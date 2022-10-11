package com.bakoconsigne.bako_collector_match.exceptions;

public class InternalServerException extends RuntimeException {

    public InternalServerException() {
    }

    public InternalServerException(String message) {
        super(message);
    }
}
