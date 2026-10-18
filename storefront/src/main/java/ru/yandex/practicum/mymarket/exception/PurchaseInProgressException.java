package ru.yandex.practicum.mymarket.exception;

public class PurchaseInProgressException extends RuntimeException {

    public PurchaseInProgressException(String message) {
        super(message);
    }
}
