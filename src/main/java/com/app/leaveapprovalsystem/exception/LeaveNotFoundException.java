package com.app.leaveapprovalsystem.exception;

public class LeaveNotFoundException extends RuntimeException {

    public LeaveNotFoundException(Long id) {
        super("Leave request not found with id: " + id);
    }

    public LeaveNotFoundException(String message) {
        super(message);
    }
}
