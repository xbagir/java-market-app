package ru.yandex.practicum.mymarket.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
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
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
@Import({RedisTestConfig.class, StubPaymentConfig.class})
class ItemCacheTest {

    private static final String MARKER = "ИЗ_КЕША";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private final List<Long> createdItemIds = new ArrayList<>();

    @AfterEach
    void isolate() {
        cartItemRepository.deleteAll().block();
        createdItemIds.forEach(id -> {
            deleteKeys(ItemCache.itemKey(id));
            itemRepository.deleteById(id).block();
        });
        createdItemIds.clear();
    }

    @Test
    void itemIsServedFromCacheWithoutSecondDbRead() throws Exception {
        long id = firstItemId();
        String key = ItemCache.itemKey(id);
        deleteKeys(key);

        String warmUpBody = getBody("/items/" + id);

        Duration ttl = redisTemplate.getExpire(key).block();
        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(Duration.ofSeconds(600));

        String json = redisTemplate.opsForValue().get(key).block();
        assertThat(json).isNotNull();
        ItemDto cached = objectMapper.readValue(json, ItemDto.class);
        ItemDto marked = new ItemDto(cached.id(), MARKER, cached.description(),
                cached.imgPath(), cached.price(), cached.count());
        redisTemplate.opsForValue()
                .set(key, objectMapper.writeValueAsString(marked), Duration.ofMinutes(5))
                .block();

        String body = getBody("/items/" + id);

        assertThat(body).contains(MARKER);
        assertThat(body).isNotEqualTo(warmUpBody);
    }

    @Test
    void catalogPageIsCached() throws Exception {
        String key = ItemCache.pageKey("", SortOption.NO, 0, 5);
        deleteKeys(key);

        getBody("/items");

        String json = redisTemplate.opsForValue().get(key).block();
        assertThat(json).isNotNull();
        ItemCache.PageValue page = objectMapper.readValue(json, ItemCache.PageValue.class);
        assertThat(page.items()).isNotEmpty();

        ItemDto first = page.items().get(0);
        ItemDto marked = new ItemDto(first.id(), MARKER, first.description(),
                first.imgPath(), first.price(), first.count());
        ItemCache.PageValue markedPage = new ItemCache.PageValue(
                page.items().stream()
                        .map(item -> item.id() == first.id() ? marked : item)
                        .toList(),
                page.total());
        redisTemplate.opsForValue()
                .set(key, objectMapper.writeValueAsString(markedPage), Duration.ofMinutes(5))
                .block();

        String body = getBody("/items");

        assertThat(body).contains(MARKER);
    }

    @Test
    void corruptedCacheEntryFallsBackToDatabase() throws Exception {
        long id = firstItemId();
        String key = ItemCache.itemKey(id);
        redisTemplate.opsForValue()
                .set(key, "{broken json", Duration.ofMinutes(5))
                .block();

        String body = getBody("/items/" + id);
        assertThat(body).isNotBlank();

        String cached = redisTemplate.opsForValue().get(key).block();
        assertThat(cached).contains("\"id\":" + id);
    }

    @Test
    void cartViewReadsItemsThroughCacheWithDbFallback() throws Exception {
        cartItemRepository.deleteAll().block();
        Item alpha = createItem("Кэш-товар Альфа");
        Item beta = createItem("Кэш-товар Бета");
        deleteKeys(ItemCache.itemKey(alpha.getId()));
        deleteKeys(ItemCache.itemKey(beta.getId()));

        addToCart(alpha.getId());
        addToCart(beta.getId());

        String coldBody = getBody("/cart/items");
        assertThat(coldBody)
                .contains(alpha.getTitle())
                .contains(beta.getTitle());
        assertThat(redisTemplate.opsForValue().get(ItemCache.itemKey(alpha.getId())).block())
                .isNotNull();
        assertThat(redisTemplate.opsForValue().get(ItemCache.itemKey(beta.getId())).block())
                .isNotNull();

        markCachedItem(alpha.getId());
        redisTemplate.delete(ItemCache.itemKey(beta.getId())).block();

        String mixedBody = getBody("/cart/items");

        assertThat(mixedBody).contains(MARKER);
        assertThat(mixedBody).doesNotContain(alpha.getTitle());
        assertThat(mixedBody).contains(beta.getTitle());
    }

    private Item createItem(String title) {
        Item item = itemRepository
                .save(new Item(title, "Описание из кэша", "images/ball.svg", 250))
                .block();
        assertThat(item).isNotNull();
        createdItemIds.add(item.getId());
        return item;
    }

    private void addToCart(long itemId) {
        webTestClient.post().uri("/items?id=" + itemId + "&action=PLUS")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    private void markCachedItem(long id) throws Exception {
        String json = redisTemplate.opsForValue().get(ItemCache.itemKey(id)).block();
        assertThat(json).isNotNull();
        ItemDto cached = objectMapper.readValue(json, ItemDto.class);
        ItemDto marked = new ItemDto(cached.id(), MARKER, cached.description(),
                cached.imgPath(), cached.price(), cached.count());
        redisTemplate.opsForValue()
                .set(ItemCache.itemKey(id), objectMapper.writeValueAsString(marked),
                        Duration.ofMinutes(5))
                .block();
    }

    private String getBody(String uri) {
        return webTestClient.get().uri(uri)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }

    private long firstItemId() {
        Item item = itemRepository.findAll().blockFirst();
        assertThat(item).isNotNull();
        return item.getId();
    }

    private void deleteKeys(String pattern) {
        redisTemplate.keys(pattern)
                .flatMap(redisTemplate::delete)
                .collectList()
                .block();
    }
}
