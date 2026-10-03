package ru.yandex.practicum.payment;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentServiceApplicationTest {

    @Autowired
    private WebTestClient client;

    @Test
    @Order(1)
    void getAccountReturnsBalance() {
        client.get().uri("/api/v1/accounts/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.accountId").value(is(1))
                .jsonPath("$.balance").value(is(100000));
    }

    @Test
    @Order(2)
    void createPaymentReturnsCreatedWithBalanceAfter() {
        client.post().uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"accountId\":1,\"amount\":500}")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.accountId").value(is(1))
                .jsonPath("$.amount").value(is(500))
                .jsonPath("$.balanceAfter").value(is(99500));
    }

    @Test
    @Order(3)
    void balanceReflectsPayment() {
        client.get().uri("/api/v1/accounts/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").value(is(99500));
    }

    @Test
    @Order(4)
    void createPaymentReturnsConflictWhenFundsInsufficient() {
        client.post().uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"accountId\":1,\"amount\":999999999}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentTypeCompatibleWith("application/problem+json")
                .expectBody()
                .jsonPath("$.title").value(is("Недостаточно средств"))
                .jsonPath("$.accountId").value(is(1));
    }

    @Test
    @Order(5)
    void getAccountReturnsNotFoundForUnknownAccount() {
        client.get().uri("/api/v1/accounts/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith("application/problem+json")
                .expectBody()
                .jsonPath("$.title").value(is("Счёт не найден"))
                .jsonPath("$.detail", containsString("99"));
    }

    @Test
    @Order(6)
    void createPaymentReturnsBadRequestForInvalidAmount() {
        client.post().uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"accountId\":1,\"amount\":0}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(7)
    void getAccountReturnsBadRequestForInvalidId() {
        client.get().uri("/api/v1/accounts/0")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(8)
    void createPaymentReturnsBadRequestForMissingField() {
        client.post().uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"accountId\":1}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
