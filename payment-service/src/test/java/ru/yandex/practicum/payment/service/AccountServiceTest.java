package ru.yandex.practicum.payment.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;
import ru.yandex.practicum.payment.exception.InvalidPaymentException;
import ru.yandex.practicum.payment.model.AccountResponse;
import ru.yandex.practicum.payment.model.PaymentResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AccountServiceTest {

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(100_000L);
    }

    @Test
    void getAccountReturnsSeededBalance() {
        StepVerifier.create(accountService.getAccount(1L))
                .assertNext(account -> {
                    assertThat(account.getAccountId()).isEqualTo(1L);
                    assertThat(account.getBalance()).isEqualTo(100_000L);
                })
                .verifyComplete();
    }

    @Test
    void getAccountProvisionsUnknownAccountWithInitialBalance() {
        StepVerifier.create(accountService.getAccount(42L))
                .assertNext(account -> {
                    assertThat(account.getAccountId()).isEqualTo(42L);
                    assertThat(account.getBalance()).isEqualTo(100_000L);
                })
                .verifyComplete();
    }

    @Test
    void accountsAreIsolatedPerUser() {
        accountService.pay(1L, 30_000L).block();

        assertThat(accountService.getAccount(1L).block().getBalance()).isEqualTo(70_000L);
        assertThat(accountService.getAccount(2L).block().getBalance()).isEqualTo(100_000L);
    }

    @Test
    void payDebitsBalance() {
        PaymentResponse first = accountService.pay(1L, 30_000L).block();
        assertThat(first.getAmount()).isEqualTo(30_000L);
        assertThat(first.getBalanceAfter()).isEqualTo(70_000L);

        PaymentResponse second = accountService.pay(1L, 20_000L).block();
        assertThat(second.getBalanceAfter()).isEqualTo(50_000L);

        AccountResponse account = accountService.getAccount(1L).block();
        assertThat(account.getBalance()).isEqualTo(50_000L);
    }

    @Test
    void payProvisionsAccountOnFirstPayment() {
        PaymentResponse payment = accountService.pay(77L, 1_490L).block();
        assertThat(payment.getAccountId()).isEqualTo(77L);
        assertThat(payment.getBalanceAfter()).isEqualTo(100_000L - 1_490L);
    }

    @Test
    void payFailsWhenFundsInsufficient() {
        StepVerifier.create(accountService.pay(1L, 100_001L))
                .expectErrorMatches(error -> error instanceof InsufficientFundsException
                        && ((InsufficientFundsException) error).getBalance() == 100_000L
                        && ((InsufficientFundsException) error).getAmount() == 100_001L)
                .verify();

        AccountResponse account = accountService.getAccount(1L).block();
        assertThat(account.getBalance()).isEqualTo(100_000L);
    }

    @Test
    void payRejectsInvalidAmount() {
        StepVerifier.create(accountService.pay(1L, 0L))
                .expectError(InvalidPaymentException.class)
                .verify();

        StepVerifier.create(accountService.pay(1L, null))
                .expectError(InvalidPaymentException.class)
                .verify();
    }

    @Test
    void payRejectsInvalidAccountId() {
        StepVerifier.create(accountService.pay(0L, 100L))
                .expectError(InvalidPaymentException.class)
                .verify();
    }

    @Test
    void concurrentDebitsDoNotLoseUpdates() {
        int operations = 200;
        long amount = 100L;

        List<PaymentResponse> results = Flux.range(0, operations)
                .flatMap(i -> accountService.pay(1L, amount).subscribeOn(Schedulers.boundedElastic()),
                        32)
                .collectList()
                .block();

        assertThat(results).hasSize(operations);
        assertThat(accountService.getAccount(1L).block().getBalance())
                .isEqualTo(100_000L - operations * amount);
    }
}
