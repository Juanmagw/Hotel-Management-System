package com.example.hotel_management_system.exceptions;

public class ResourceNotValidException extends RuntimeException {
    public ResourceNotValidException(String message) {
        super(message);
    }
}
