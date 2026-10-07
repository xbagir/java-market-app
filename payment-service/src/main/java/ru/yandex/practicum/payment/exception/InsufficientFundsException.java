package ru.yandex.practicum.payment.exception;

public class InsufficientFundsException extends RuntimeException {

    private final long accountId;
    private final long balance;
    private final long amount;

    public InsufficientFundsException(long accountId, long balance, long amount) {
        super("Недостаточно средств на счёте " + accountId + ": баланс " + balance
                + ", требуется " + amount);
        this.accountId = accountId;
        this.balance = balance;
        this.amount = amount;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getBalance() {
        return balance;
    }

    public long getAmount() {
        return amount;
    }
}
