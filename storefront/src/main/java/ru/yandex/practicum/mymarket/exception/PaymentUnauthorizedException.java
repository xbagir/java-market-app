package ru.yandex.practicum.mymarket.exception;

public class PaymentUnauthorizedException extends RuntimeException {

    public PaymentUnauthorizedException(String message) {
        super(message);
    }

    public PaymentUnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
