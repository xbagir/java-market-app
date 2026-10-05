package ru.yandex.practicum.mymarket.service;

import reactor.core.publisher.Mono;

public interface PaymentGateway {

    Mono<Long> getBalance();

    Mono<Void> charge(long amount);
}
