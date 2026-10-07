package ru.yandex.practicum.payment.exception;

public class AccountNotFoundException extends RuntimeException {

    private final long accountId;

    public AccountNotFoundException(long accountId) {
        super("Счёт с id " + accountId + " не найден");
        this.accountId = accountId;
    }

    public long getAccountId() {
        return accountId;
    }
}
