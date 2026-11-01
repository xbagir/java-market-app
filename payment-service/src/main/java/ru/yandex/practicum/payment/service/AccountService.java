package ru.yandex.practicum.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ru.yandex.practicum.payment.exception.InsufficientFundsException;
import ru.yandex.practicum.payment.exception.InvalidPaymentException;
import ru.yandex.practicum.payment.model.AccountResponse;
import ru.yandex.practicum.payment.model.PaymentResponse;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final Map<Long, Long> balances = new ConcurrentHashMap<>();
    private final long initialBalance;

    public AccountService(@Value("${app.payments.initial-balance:100000}") long initialBalance) {
        this.initialBalance = initialBalance;
        log.info("Accounts are provisioned on demand with initial balance {}", initialBalance);
    }

    public Mono<AccountResponse> getAccount(Long accountId) {
        return Mono.fromCallable(() -> {
            long balance = balances.computeIfAbsent(accountId, this::provision);
            return new AccountResponse(accountId, balance);
        });
    }

    public Mono<PaymentResponse> pay(Long accountId, Long amount) {
        return Mono.fromCallable(() -> debit(accountId, amount));
    }

    private long provision(long accountId) {
        if (accountId < 1) {
            throw new InvalidPaymentException("Идентификатор счёта должен быть больше нуля");
        }
        log.info("Provisioned account {} with balance {}", accountId, initialBalance);
        return initialBalance;
    }

    private PaymentResponse debit(Long accountId, Long amount) {
        if (accountId == null || accountId < 1) {
            throw new InvalidPaymentException("Идентификатор счёта должен быть больше нуля");
        }
        if (amount == null || amount < 1) {
            throw new InvalidPaymentException("Сумма списания должна быть не меньше 1 рубля");
        }
        long balanceAfter = balances.compute(accountId, (id, current) -> {
            long balance = (current != null) ? current : provision(id);
            if (balance < amount) {
                throw new InsufficientFundsException(accountId, balance, amount);
            }
            return balance - amount;
        });
        log.info("Debited {} from account {}, balance after {}", amount, accountId, balanceAfter);
        return new PaymentResponse(accountId, amount, balanceAfter);
    }
}
