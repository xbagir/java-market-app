package ru.yandex.practicum.mymarket.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import ru.yandex.practicum.mymarket.config.PaymentClientConfig;
import ru.yandex.practicum.mymarket.config.PaymentProperties;
import ru.yandex.practicum.mymarket.exception.InsufficientFundsException;
import ru.yandex.practicum.mymarket.exception.PaymentUnavailableException;
import ru.yandex.practicum.mymarket.payment.ApiClient;
import ru.yandex.practicum.mymarket.payment.api.AccountsApi;
import ru.yandex.practicum.mymarket.payment.api.PaymentsApi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceClientTest {

    private HttpServer server;
    private String basePath;
    private PaymentServiceClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/accounts/1", exchange -> respond(exchange, 200,
                "{\"accountId\":1,\"balance\":100000}"));
        server.createContext("/api/v1/accounts/99", exchange -> respond(exchange, 404,
                "{\"status\":404,\"title\":\"Счёт не найден\"}"));
        server.createContext("/api/v1/payments", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (body.contains("999999999")) {
                respond(exchange, 409,
                        "{\"status\":409,\"title\":\"Недостаточно средств\"}");
            } else {
                respond(exchange, 201,
                        "{\"accountId\":1,\"amount\":500,\"balanceAfter\":99500}");
            }
        });
        server.start();
        basePath = "http://127.0.0.1:" + server.getAddress().getPort();
        client = clientWith(accountId(basePath, 1L));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static PaymentProperties accountId(String basePath, Long accountId) {
        return new PaymentProperties(basePath, accountId, Duration.ofSeconds(5));
    }

    private static PaymentServiceClient clientWith(PaymentProperties properties) {
        ApiClient apiClient = new PaymentClientConfig()
                .paymentApiClient(WebClient.builder(), properties);
        return new PaymentServiceClient(new AccountsApi(apiClient), new PaymentsApi(apiClient),
                properties);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }

    @Test
    void getBalanceReturnsAccountBalance() {
        assertThat(client.getBalance().block()).isEqualTo(100_000L);
    }

    @Test
    void chargeCompletesOnSuccessfulPayment() {
        assertThat(client.charge(500L).block()).isNull();
    }

    @Test
    void chargeMapsConflictToInsufficientFunds() {
        StepVerifier.create(client.charge(999_999_999L))
                .expectError(InsufficientFundsException.class)
                .verify();
    }

    @Test
    void getBalanceMapsNotFoundToPaymentUnavailable() {
        PaymentServiceClient missingAccount = clientWith(accountId(basePath, 99L));

        StepVerifier.create(missingAccount.getBalance())
                .expectError(PaymentUnavailableException.class)
                .verify();
    }

    @Test
    void getBalanceFailsFastWhenServiceIsDown() {
        PaymentServiceClient down = clientWith(
                new PaymentProperties("http://127.0.0.1:1", 1L, Duration.ofSeconds(5)));

        StepVerifier.create(down.getBalance())
                .expectError(PaymentUnavailableException.class)
                .verify();
    }

    @Test
    void getBalanceMapsDelayedResponseToPaymentUnavailable() throws IOException {
        ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "slow-payment-server");
            thread.setDaemon(true);
            return thread;
        });
        HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slow.setExecutor(executor);
        slow.createContext("/api/v1/accounts/1", exchange -> {
            try {
                Thread.sleep(2000);
                respond(exchange, 200, "{\"accountId\":1,\"balance\":100000}");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // клиент уже отключился, не дождавшись ответа
            }
        });
        slow.start();
        try {
            PaymentProperties properties = new PaymentProperties(
                    "http://127.0.0.1:" + slow.getAddress().getPort(), 1L,
                    Duration.ofMillis(500));
            PaymentServiceClient delayed = clientWith(properties);

            StepVerifier.create(delayed.getBalance())
                    .expectError(PaymentUnavailableException.class)
                    .verify(Duration.ofSeconds(3));
        } finally {
            slow.stop(0);
            executor.shutdownNow();
        }
    }
}
