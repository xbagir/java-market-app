package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.practicum.mymarket.cache.ItemCache;
import ru.yandex.practicum.mymarket.config.RedisTestConfig;
import ru.yandex.practicum.mymarket.config.StubPaymentConfig;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@SpringBootTest
@AutoConfigureWebTestClient
@Import({RedisTestConfig.class, StubPaymentConfig.class})
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

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    private WebTestClient buyerClient;

    private Item ball;
    private Item doll;

    @BeforeEach
    void setUp() {
        buyerClient = webTestClient.mutateWith(mockUser("user").roles("USER")).mutateWith(csrf());
        redisTemplate.keys(ItemCache.KEY_PREFIX + "*")
                .flatMap(redisTemplate::delete)
                .collectList()
                .block();
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
    void anonymousCatalogHidesPurchaseControls() {
        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Войти"))
                .value(not(containsString("Выйти")));
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

        buyerClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч футбольный"))
                .value(containsString("Кукла «Алиса»"))
                .value(containsString(String.valueOf(expectedTotal)));

        buyerClient.post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location",
                        location -> org.assertj.core.api.Assertions.assertThat(location)
                                .startsWith("/orders/"));

        Long orderId = orderRepository.findAll()
                .map(ru.yandex.practicum.mymarket.model.Order::getId)
                .blockFirst();

        buyerClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Заказ №"));

        buyerClient.get().uri("/orders/" + orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Заказ №" + orderId));

        buyerClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(not(containsString("Мяч футбольный")));
    }

    @Test
    void cartQuantityActionsWork() {
        addToCart(ball);
        addToCart(ball);

        buyerClient.post().uri("/items?id=" + ball.getId() + "&action=MINUS")
                .exchange()
                .expectStatus().is3xxRedirection();

        buyerClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString(String.valueOf(ball.getPrice())));

        buyerClient.post().uri("/cart/items?id=" + ball.getId() + "&action=DELETE")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/cart/items");

        buyerClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(not(containsString("Мяч футбольный")));
    }

    private void addToCart(Item item) {
        buyerClient.post().uri("/items?id=" + item.getId() + "&action=PLUS")
                .exchange()
                .expectStatus().is3xxRedirection();
    }
}
