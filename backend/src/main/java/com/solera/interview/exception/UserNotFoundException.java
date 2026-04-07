package com.solera.interview.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User not found for id: " + userId);
    }
}
