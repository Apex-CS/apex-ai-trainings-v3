package com.owasp.it.service;

public class AppServerNotFoundException extends RuntimeException {

    public AppServerNotFoundException(String message) {
        super(message);
    }
}
