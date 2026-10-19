package ru.yandex.practicum.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest
@AutoConfigureWebTestClient
class PaymentServiceApplicationTest {

    private static final SimpleGrantedAuthority READ =
            new SimpleGrantedAuthority("SCOPE_payment.read");
    private static final SimpleGrantedAuthority WRITE =
            new SimpleGrantedAuthority("SCOPE_payment.write");

    @Autowired
    private WebTestClient client;

    private WebTestClient asReader() {
        return client.mutateWith(mockJwt().authorities(READ));
    }

    private WebTestClient asWriter() {
        return client.mutateWith(mockJwt().authorities(WRITE));
    }

    @Test
    void healthEndpointIsPublic() {
        client.get().uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").value(is("UP"));
    }

    @Test
    void requestsWithoutTokenAreUnauthorized() {
        client.get().uri("/api/v1/accounts/1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getAccountReturnsProvisionedBalance() {
        asReader().get().uri("/api/v1/accounts/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.accountId").value(is(1))
                .jsonPath("$.balance").value(is(100000));
    }

    @Test
    void paymentIsDebitedAndReflectedInBalance() {
        asWriter().post().uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"accountId\":2,\"amount\":500}")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.accountId").value(is(2))
                .jsonPath("$.amount").value(is(500))
                .jsonPath("$.balanceAfter").value(is(99500));

        asReader().get().uri("/api/v1/accounts/2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").value(is(99500));
    }

    @Test
    void createPaymentReturnsConflictWhenFundsInsufficient() {
        asWriter().post().uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"accountId\":3,\"amount\":999999999}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentTypeCompatibleWith("application/problem+json")
                .expectBody()
                .jsonPath("$.title").value(is("Недостаточно средств"))
                .jsonPath("$.accountId").value(is(3));
    }

    @Test
    void getAccountProvisionsUnknownAccount() {
        asReader().get().uri("/api/v1/accounts/99")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountId").value(is(99))
                .jsonPath("$.balance").value(is(100000));
    }

    @Test
    void createPaymentReturnsBadRequestForInvalidAmount() {
        asWriter().post().uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"accountId\":4,\"amount\":0}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getAccountReturnsBadRequestForInvalidId() {
        asReader().get().uri("/api/v1/accounts/0")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createPaymentReturnsBadRequestForMissingField() {
        asWriter().post().uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"accountId\":1}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void readScopeCannotCreatePayment() {
        client.mutateWith(mockJwt().authorities(READ))
                .post().uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"accountId\":1,\"amount\":500}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void writeScopeCannotReadAccount() {
        client.mutateWith(mockJwt().authorities(WRITE))
                .get().uri("/api/v1/accounts/1")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void invalidPaymentAccountIsRejected() {
        asWriter().post().uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"accountId\":0,\"amount\":100}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
