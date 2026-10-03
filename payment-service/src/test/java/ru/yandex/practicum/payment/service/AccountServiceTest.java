package ru.yandex.practicum.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.exception.AccountNotFoundException;
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
    void getAccountThrowsForUnknownAccount() {
        StepVerifier.create(accountService.getAccount(42L))
                .expectError(AccountNotFoundException.class)
                .verify();
    }

    @Test
    void payDebitsBalanceAtomically() {
        PaymentResponse first = accountService.pay(1L, 30_000L).block();
        assertThat(first.getAmount()).isEqualTo(30_000L);
        assertThat(first.getBalanceAfter()).isEqualTo(70_000L);

        PaymentResponse second = accountService.pay(1L, 20_000L).block();
        assertThat(second.getBalanceAfter()).isEqualTo(50_000L);

        AccountResponse account = accountService.getAccount(1L).block();
        assertThat(account.getBalance()).isEqualTo(50_000L);
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
    void payFailsForUnknownAccount() {
        StepVerifier.create(accountService.pay(42L, 100L))
                .expectError(AccountNotFoundException.class)
                .verify();
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
}
