package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import ru.yandex.practicum.mymarket.model.Item;

import java.util.Collection;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ItemRepository extends R2dbcRepository<Item, Long> {

    @Query("SELECT * FROM items WHERE id IN (:ids) ORDER BY id")
    Flux<Item> findAllByIdOrdered(Collection<Long> ids);

    @Query("SELECT * FROM items WHERE LOWER(title || ' ' || description) LIKE :pattern")
    Flux<Item> searchByPattern(String pattern, Pageable pageable);

    @Query("SELECT COUNT(*) FROM items WHERE LOWER(title || ' ' || description) LIKE :pattern")
    Mono<Long> countByPattern(String pattern);
}
