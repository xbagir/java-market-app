package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import ru.yandex.practicum.mymarket.model.Item;

import java.util.Collection;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ItemRepository extends R2dbcRepository<Item, Long> {

    @Query("SELECT * FROM items WHERE id IN (:ids) ORDER BY id")
    Flux<Item> findAllByIdOrdered(Collection<Long> ids);

    @Query("SELECT * FROM items WHERE LOWER(title || ' ' || description) LIKE :pattern "
            + "ORDER BY id LIMIT :limit OFFSET :offset")
    Flux<Item> searchOrderById(String pattern, int limit, long offset);

    @Query("SELECT * FROM items WHERE LOWER(title || ' ' || description) LIKE :pattern "
            + "ORDER BY title LIMIT :limit OFFSET :offset")
    Flux<Item> searchOrderByTitle(String pattern, int limit, long offset);

    @Query("SELECT * FROM items WHERE LOWER(title || ' ' || description) LIKE :pattern "
            + "ORDER BY price LIMIT :limit OFFSET :offset")
    Flux<Item> searchOrderByPrice(String pattern, int limit, long offset);

    @Query("SELECT COUNT(*) FROM items WHERE LOWER(title || ' ' || description) LIKE :pattern")
    Mono<Long> countByPattern(String pattern);
}
