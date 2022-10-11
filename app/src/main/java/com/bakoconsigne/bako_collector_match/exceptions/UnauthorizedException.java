package com.bakoconsigne.bako_collector_match.exceptions;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
