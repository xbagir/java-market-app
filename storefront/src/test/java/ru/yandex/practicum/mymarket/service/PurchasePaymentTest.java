package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.practicum.mymarket.config.RedisTestConfig;
import ru.yandex.practicum.mymarket.config.StubPaymentConfig;
import ru.yandex.practicum.mymarket.config.StubPaymentGateway;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureWebTestClient
@Import({RedisTestConfig.class, StubPaymentConfig.class})
class PurchasePaymentTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private StubPaymentGateway paymentGateway;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Item ball;

    @BeforeEach
    void setUp() {
        paymentGateway.reset();
        orderItemRepository.deleteAll()
                .then(cartItemRepository.deleteAll())
                .then(orderRepository.deleteAll())
                .then(itemRepository.deleteAll())
                .then(itemRepository.save(new Item("Мяч футбольный", "Круглый мяч для игры",
                        "images/ball.svg", 1490)))
                .block();
        ball = itemRepository.findAll().blockFirst();
    }

    @AfterEach
    void tearDown() {
        paymentGateway.reset();
    }

    @Test
    void successfulPurchaseChargesExactlyCartTotal() {
        addToCart(ball);
        long ordersBefore = ordersCount();

        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location",
                        location -> org.assertj.core.api.Assertions.assertThat(location)
                                .startsWith("/orders/"));

        assertThat(paymentGateway.charges()).containsExactly(ball.getPrice());
        assertThat(paymentGateway.getBalance().block())
                .isEqualTo(100_000L - ball.getPrice());
        assertThat(ordersCount()).isEqualTo(ordersBefore + 1);
    }

    @Test
    void purchaseIsRejectedWith402WhenBalanceTooLow() {
        paymentGateway.setBalance(10);
        addToCart(ball);
        long ordersBefore = ordersCount();

        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().isEqualTo(402)
                .expectBody(String.class).value(containsString("Недостаточно средств"));

        assertThat(paymentGateway.charges()).isEmpty();
        assertThat(paymentGateway.getBalance().block()).isEqualTo(10L);
        assertThat(ordersCount()).isEqualTo(ordersBefore);
    }

    @Test
    void purchaseFailsWith503WhenPaymentServiceIsDown() {
        paymentGateway.setServiceDown(true);
        addToCart(ball);
        long ordersBefore = ordersCount();

        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody(String.class).value(containsString("временно недоступен"));

        assertThat(paymentGateway.charges()).isEmpty();
        assertThat(ordersCount()).isEqualTo(ordersBefore);
    }

    @Test
    void cartPageShowsCurrentBalance() {
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Баланс: 100000 руб."));
    }

    @Test
    void cartPageShowsInsufficientFundsWhenBalanceTooLow() {
        paymentGateway.setBalance(10);
        addToCart(ball);

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Недостаточно средств на балансе"))
                .value(containsString("disabled=\"disabled\""));
    }

    @Test
    void cartPageShowsUnavailableWhenPaymentServiceIsDown() {
        paymentGateway.setServiceDown(true);
        addToCart(ball);

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Сервис платежей недоступен"))
                .value(containsString("disabled=\"disabled\""));
    }

    private void addToCart(Item item) {
        webTestClient.post().uri("/items?id=" + item.getId() + "&action=PLUS")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    private long ordersCount() {
        List<Order> orders = orderRepository.findAllByOrderByIdDesc().collectList().block();
        return orders == null ? 0L : orders.size();
    }
}
