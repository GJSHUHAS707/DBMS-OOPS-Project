package com.cinephile.exception;

public class SeatAlreadyBookedException extends CinephileException {
    public SeatAlreadyBookedException(String message) {
        super(message);
    }
}
