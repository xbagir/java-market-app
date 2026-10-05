package ru.yandex.practicum.mymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import ru.yandex.practicum.mymarket.config.PaymentProperties;
import ru.yandex.practicum.mymarket.exception.InsufficientFundsException;
import ru.yandex.practicum.mymarket.exception.PaymentUnavailableException;
import ru.yandex.practicum.mymarket.payment.api.AccountsApi;
import ru.yandex.practicum.mymarket.payment.api.PaymentsApi;
import ru.yandex.practicum.mymarket.payment.model.AccountResponse;
import ru.yandex.practicum.mymarket.payment.model.PaymentRequest;

import reactor.core.publisher.Mono;

@Component
public class PaymentServiceClient implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceClient.class);

    private final AccountsApi accountsApi;
    private final PaymentsApi paymentsApi;
    private final PaymentProperties properties;

    public PaymentServiceClient(AccountsApi accountsApi,
                                PaymentsApi paymentsApi,
                                PaymentProperties properties) {
        this.accountsApi = accountsApi;
        this.paymentsApi = paymentsApi;
        this.properties = properties;
    }

    @Override
    public Mono<Long> getBalance() {
        return accountsApi.getAccount(properties.accountId())
                .map(AccountResponse::getBalance)
                .onErrorMap(WebClientResponseException.class, this::unavailable)
                .onErrorMap(WebClientRequestException.class, this::unavailable);
    }

    @Override
    public Mono<Void> charge(long amount) {
        PaymentRequest request = new PaymentRequest()
                .accountId(properties.accountId())
                .amount(amount);
        return paymentsApi.createPayment(request)
                .doOnNext(payment -> log.info("Charged {} from account {}, balance after {}",
                        payment.getAmount(), payment.getAccountId(), payment.getBalanceAfter()))
                .then()
                .onErrorMap(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                        return new InsufficientFundsException(amount, null);
                    }
                    return unavailable(ex);
                })
                .onErrorMap(WebClientRequestException.class, this::unavailable);
    }

    private PaymentUnavailableException unavailable(Exception ex) {
        log.error("Payment service request failed: {}", ex.getMessage());
        return new PaymentUnavailableException("Сервис платежей временно недоступен", ex);
    }
}
