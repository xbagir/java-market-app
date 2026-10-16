package ru.yandex.practicum.mymarket.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortOption;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
public class ItemCache {

    private static final Logger log = LoggerFactory.getLogger(ItemCache.class);

    public static final String KEY_PREFIX = "storefront:";

    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public ItemCache(ReactiveStringRedisTemplate redis, ObjectMapper objectMapper,
                     @Value("${app.cache.item-ttl:10m}") Duration ttl) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    public static String itemKey(long id) {
        return KEY_PREFIX + "item:" + id;
    }

    public static String pageKey(String search, SortOption sort, int pageNumber, int pageSize) {
        return KEY_PREFIX + "page:" + search + ":" + sort.name() + ":" + pageNumber + ":" + pageSize;
    }

    public <T> Mono<T> get(String key, Class<T> type) {
        return redis.opsForValue().get(key)
                .flatMap(json -> decode(json, type))
                .doOnNext(value -> log.debug("Cache hit {}", key))
                .onErrorResume(error -> {
                    log.warn("Cache read failed for {}: {}", key, error.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<Void> put(String key, Object value) {
        String json;
        try {
            json = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            log.warn("Cache write skipped for {}: {}", key, error.getMessage());
            return Mono.empty();
        }
        return redis.opsForValue().set(key, json, ttl)
                .doOnSuccess(ok -> log.debug("Cache stored {}", key))
                .then()
                .onErrorResume(error -> {
                    log.warn("Cache write failed for {}: {}", key, error.getMessage());
                    return Mono.empty();
                });
    }

    private <T> Mono<T> decode(String json, Class<T> type) {
        try {
            return Mono.just(objectMapper.readValue(json, type));
        } catch (JsonProcessingException error) {
            log.warn("Cache entry {} is corrupted and will be ignored: {}", type.getSimpleName(),
                    error.getMessage());
            return Mono.empty();
        }
    }

    public record PageValue(List<ItemDto> items, long total) {
    }
}
