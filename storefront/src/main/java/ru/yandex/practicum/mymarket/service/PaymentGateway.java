package ru.yandex.practicum.mymarket.service;

import reactor.core.publisher.Mono;

public interface PaymentGateway {

    Mono<Long> getBalance(long accountId);

    Mono<Void> charge(long accountId, long amount);
}
