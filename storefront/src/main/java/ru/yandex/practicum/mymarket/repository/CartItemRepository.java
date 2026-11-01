package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import ru.yandex.practicum.mymarket.model.CartItem;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {

    @Query("SELECT * FROM cart_items WHERE user_id = :userId AND item_id = :itemId")
    Mono<CartItem> findByUserIdAndItemId(long userId, long itemId);

    @Query("SELECT * FROM cart_items WHERE user_id = :userId")
    Flux<CartItem> findAllByUserId(long userId);

    @Modifying
    @Query("UPDATE cart_items SET quantity = quantity + 1 "
            + "WHERE user_id = :userId AND item_id = :itemId")
    Mono<Integer> incrementQuantity(long userId, long itemId);

    @Modifying
    @Query("UPDATE cart_items SET quantity = quantity - 1 "
            + "WHERE user_id = :userId AND item_id = :itemId AND quantity > 1")
    Mono<Integer> decrementQuantity(long userId, long itemId);
}
