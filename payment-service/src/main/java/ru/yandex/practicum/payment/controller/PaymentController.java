package ru.yandex.practicum.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import ru.yandex.practicum.payment.api.AccountsApi;
import ru.yandex.practicum.payment.api.PaymentsApi;
import ru.yandex.practicum.payment.model.AccountResponse;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;
import ru.yandex.practicum.payment.service.AccountService;

import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@Validated
public class PaymentController implements AccountsApi, PaymentsApi {

    private final AccountService accountService;

    public PaymentController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> getAccount(
            @Min(value = 1L) @PathVariable("accountId") Long accountId,
            ServerWebExchange exchange) {
        return accountService.getAccount(accountId).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> createPayment(
            @Valid @RequestBody Mono<PaymentRequest> paymentRequest,
            ServerWebExchange exchange) {
        return paymentRequest.flatMap(request -> accountService
                .pay(request.getAccountId(), request.getAmount())
                .map(payment -> ResponseEntity.status(HttpStatus.CREATED).body(payment)));
    }
}
