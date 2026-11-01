package ru.yandex.practicum.mymarket.exception;

public class UsernameAlreadyUsedException extends RuntimeException {

    public UsernameAlreadyUsedException(String message) {
        super(message);
    }
}
