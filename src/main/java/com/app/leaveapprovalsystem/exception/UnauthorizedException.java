package com.app.leaveapprovalsystem.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("You are not authorized to perform this action");
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
