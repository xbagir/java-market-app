package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@SpringBootTest
@AutoConfigureWebTestClient
class MyMarketAppApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Item ball;
    private Item doll;

    @BeforeEach
    void setUp() {
        List<Item> saved = orderItemRepository.deleteAll()
                .then(cartItemRepository.deleteAll())
                .then(orderRepository.deleteAll())
                .then(itemRepository.deleteAll())
                .thenMany(itemRepository.saveAll(List.of(
                        new Item("Мяч футбольный", "Круглый мяч для игры", "images/ball.svg", 1490),
                        new Item("Кукла «Алиса»", "Нарядная кукла", "images/doll.svg", 1890))))
                .collectList()
                .block();
        ball = saved.stream().filter(i -> i.getTitle().startsWith("Мяч")).findFirst().orElseThrow();
        doll = saved.stream().filter(i -> i.getTitle().startsWith("Кукла")).findFirst().orElseThrow();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void itemsPageShowsCatalog() {
        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч футбольный"))
                .value(containsString("Кукла «Алиса»"));
    }

    @Test
    void searchFiltersCatalog() {
        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("search", "мяч").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч футбольный"))
                .value(not(containsString("Кукла «Алиса»")));
    }

    @Test
    void fullPurchaseFlow() {
        addToCart(ball);
        addToCart(doll);
        addToCart(ball);

        long expectedTotal = 2L * ball.getPrice() + doll.getPrice();

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч футбольный"))
                .value(containsString("Кукла «Алиса»"))
                .value(containsString(String.valueOf(expectedTotal)));

        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location",
                        location -> org.assertj.core.api.Assertions.assertThat(location)
                                .startsWith("/orders/"));

        Long orderId = orderRepository.findAllByOrderByIdDesc()
                .map(ru.yandex.practicum.mymarket.model.Order::getId)
                .blockFirst();

        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Заказ №"));

        webTestClient.get().uri("/orders/" + orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Заказ №" + orderId));

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(not(containsString("Мяч футбольный")));
    }

    @Test
    void cartQuantityActionsWork() {
        addToCart(ball);
        addToCart(ball);

        webTestClient.post().uri("/items?id=" + ball.getId() + "&action=MINUS")
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString(String.valueOf(ball.getPrice())));

        webTestClient.post().uri("/cart/items?id=" + ball.getId() + "&action=DELETE")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/cart/items");

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(not(containsString("Мяч футбольный")));
    }

    private void addToCart(Item item) {
        webTestClient.post().uri("/items?id=" + item.getId() + "&action=PLUS")
                .exchange()
                .expectStatus().is3xxRedirection();
    }
}
