package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import ru.yandex.practicum.mymarket.model.CartItem;

import reactor.core.publisher.Mono;

public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {

    @Query("SELECT * FROM cart_items WHERE item_id = :itemId")
    Mono<CartItem> findByItemId(long itemId);
}
