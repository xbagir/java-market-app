package ru.yandex.practicum.mymarket.exception;

public class InsufficientFundsException extends RuntimeException {

    private final long requiredAmount;
    private final Long currentBalance;

    public InsufficientFundsException(long requiredAmount, Long currentBalance) {
        super("Недостаточно средств: требуется " + requiredAmount
                + (currentBalance != null ? ", баланс " + currentBalance : ""));
        this.requiredAmount = requiredAmount;
        this.currentBalance = currentBalance;
    }

    public long getRequiredAmount() {
        return requiredAmount;
    }

    public Long getCurrentBalance() {
        return currentBalance;
    }
}
