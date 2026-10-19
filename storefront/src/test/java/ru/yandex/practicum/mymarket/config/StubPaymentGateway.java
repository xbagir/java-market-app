package ru.yandex.practicum.mymarket.config;

import ru.yandex.practicum.mymarket.exception.InsufficientFundsException;
import ru.yandex.practicum.mymarket.exception.PaymentUnavailableException;
import ru.yandex.practicum.mymarket.service.PaymentGateway;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class StubPaymentGateway implements PaymentGateway {

    public static final long INITIAL_BALANCE = 100_000L;

    private final AtomicLong defaultBalance = new AtomicLong(INITIAL_BALANCE);
    private final Map<Long, Long> balances = new ConcurrentHashMap<>();
    private final List<Charge> charges = new CopyOnWriteArrayList<>();
    private final AtomicBoolean serviceDown = new AtomicBoolean();

    @Override
    public Mono<Long> getBalance(long accountId) {
        if (serviceDown.get()) {
            return Mono.error(new PaymentUnavailableException("stub: payment service is down"));
        }
        return Mono.fromSupplier(() -> balanceOf(accountId));
    }

    @Override
    public Mono<Void> charge(long accountId, long amount) {
        return Mono.defer(() -> {
            if (serviceDown.get()) {
                return Mono.error(new PaymentUnavailableException("stub: payment service is down"));
            }
            long current = balanceOf(accountId);
            if (amount > current) {
                return Mono.error(new InsufficientFundsException(amount, current));
            }
            balances.put(accountId, current - amount);
            charges.add(new Charge(accountId, amount));
            return Mono.empty();
        });
    }

    private long balanceOf(long accountId) {
        return balances.computeIfAbsent(accountId, id -> defaultBalance.get());
    }

    public void reset() {
        defaultBalance.set(INITIAL_BALANCE);
        balances.clear();
        charges.clear();
        serviceDown.set(false);
    }

    public void setBalance(long newBalance) {
        defaultBalance.set(newBalance);
        balances.clear();
    }

    public void setBalance(long accountId, long newBalance) {
        balances.put(accountId, newBalance);
    }

    public void setServiceDown(boolean down) {
        serviceDown.set(down);
    }

    public List<Long> charges() {
        return charges.stream().map(Charge::amount).toList();
    }

    public List<Charge> chargesFor(long accountId) {
        return charges.stream().filter(charge -> charge.accountId() == accountId).toList();
    }

    public record Charge(long accountId, long amount) {
    }
}
