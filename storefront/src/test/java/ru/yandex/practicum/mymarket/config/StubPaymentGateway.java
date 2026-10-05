package ru.yandex.practicum.mymarket.config;

import ru.yandex.practicum.mymarket.exception.InsufficientFundsException;
import ru.yandex.practicum.mymarket.service.PaymentGateway;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class StubPaymentGateway implements PaymentGateway {

    private static final long INITIAL_BALANCE = 100_000L;

    private final AtomicLong balance = new AtomicLong(INITIAL_BALANCE);
    private final List<Long> charges = new CopyOnWriteArrayList<>();

    @Override
    public Mono<Long> getBalance() {
        return Mono.fromSupplier(balance::get);
    }

    @Override
    public Mono<Void> charge(long amount) {
        return Mono.defer(() -> {
            long current = balance.get();
            if (amount > current) {
                return Mono.error(new InsufficientFundsException(amount, current));
            }
            balance.addAndGet(-amount);
            charges.add(amount);
            return Mono.empty();
        });
    }

    public void reset() {
        balance.set(INITIAL_BALANCE);
        charges.clear();
    }

    public List<Long> charges() {
        return List.copyOf(charges);
    }
}
