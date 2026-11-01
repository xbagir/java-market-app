package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import ru.yandex.practicum.mymarket.model.OrderItem;

import java.util.Collection;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderItemRepository extends R2dbcRepository<OrderItem, Long> {

    @Query("SELECT * FROM order_items WHERE order_id = :orderId")
    Flux<OrderItem> findByOrderId(long orderId);

    @Query("SELECT * FROM order_items WHERE order_id IN (:orderId)")
    Flux<OrderItem> findByOrderIdIn(Collection<Long> orderId);

    @Modifying
    @Query("DELETE FROM order_items WHERE order_id = :orderId")
    Mono<Integer> deleteByOrderId(long orderId);
}
