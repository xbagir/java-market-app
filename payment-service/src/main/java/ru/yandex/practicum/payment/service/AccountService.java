package ru.yandex.practicum.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ru.yandex.practicum.payment.exception.AccountNotFoundException;
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

    private static final long DEMO_ACCOUNT_ID = 1L;

    private final Map<Long, Long> balances = new ConcurrentHashMap<>();

    public AccountService(@Value("${app.payments.initial-balance:100000}") long initialBalance) {
        balances.put(DEMO_ACCOUNT_ID, initialBalance);
        log.info("Seeded demo account {} with balance {}", DEMO_ACCOUNT_ID, initialBalance);
    }

    public Mono<AccountResponse> getAccount(Long accountId) {
        return Mono.fromCallable(() -> {
            Long balance = balances.get(accountId);
            if (balance == null) {
                throw new AccountNotFoundException(accountId);
            }
            return new AccountResponse(accountId, balance);
        });
    }

    public Mono<PaymentResponse> pay(Long accountId, Long amount) {
        return Mono.fromCallable(() -> debit(accountId, amount));
    }

    private PaymentResponse debit(Long accountId, Long amount) {
        if (accountId == null || accountId < 1) {
            throw new InvalidPaymentException("Идентификатор счёта должен быть больше нуля");
        }
        if (amount == null || amount < 1) {
            throw new InvalidPaymentException("Сумма списания должна быть не меньше 1 рубля");
        }
        long[] updated = {-1L};
        balances.compute(accountId, (id, current) -> {
            if (current == null) {
                return null;
            }
            if (current >= amount) {
                updated[0] = current - amount;
                return updated[0];
            }
            return current;
        });
        if (updated[0] >= 0) {
            log.info("Debited {} from account {}, balance after {}", amount, accountId, updated[0]);
            return new PaymentResponse(accountId, amount, updated[0]);
        }
        Long current = balances.get(accountId);
        if (current == null) {
            throw new AccountNotFoundException(accountId);
        }
        throw new InsufficientFundsException(accountId, current, amount);
    }
}
