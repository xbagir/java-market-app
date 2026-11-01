package ru.yandex.practicum.mymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import ru.yandex.practicum.mymarket.exception.InsufficientFundsException;
import ru.yandex.practicum.mymarket.exception.PaymentUnauthorizedException;
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

    public PaymentServiceClient(AccountsApi accountsApi, PaymentsApi paymentsApi) {
        this.accountsApi = accountsApi;
        this.paymentsApi = paymentsApi;
    }

    @Override
    public Mono<Long> getBalance(long accountId) {
        return accountsApi.getAccount(accountId)
                .map(AccountResponse::getBalance)
                .onErrorMap(PaymentServiceClient::isTimeout, this::unavailable)
                .onErrorMap(WebClientResponseException.class, this::mapResponseError)
                .onErrorMap(WebClientRequestException.class, this::unavailable);
    }

    @Override
    public Mono<Void> charge(long accountId, long amount) {
        PaymentRequest request = new PaymentRequest()
                .accountId(accountId)
                .amount(amount);
        return paymentsApi.createPayment(request)
                .doOnNext(payment -> log.info("Charged {} from account {}, balance after {}",
                        payment.getAmount(), payment.getAccountId(), payment.getBalanceAfter()))
                .then()
                .onErrorMap(PaymentServiceClient::isTimeout, this::unavailable)
                .onErrorMap(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                        return new InsufficientFundsException(amount, null);
                    }
                    return mapResponseError(ex);
                })
                .onErrorMap(WebClientRequestException.class, this::unavailable);
    }

    private Throwable mapResponseError(WebClientResponseException ex) {
        int status = ex.getStatusCode().value();
        if (status == HttpStatus.UNAUTHORIZED.value()
                || status == HttpStatus.FORBIDDEN.value()) {
            log.error("Payment service rejected the storefront client: {}", status);
            return new PaymentUnauthorizedException(
                    "Витрина не авторизована для обращения к сервису платежей", ex);
        }
        return unavailable(ex);
    }

    private static boolean isTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof java.util.concurrent.TimeoutException
                    || current instanceof io.netty.handler.timeout.TimeoutException) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return false;
    }

    private PaymentUnavailableException unavailable(Throwable ex) {
        log.error("Payment service request failed: {}", ex.getMessage());
        return new PaymentUnavailableException("Сервис платежей временно недоступен", ex);
    }
}
