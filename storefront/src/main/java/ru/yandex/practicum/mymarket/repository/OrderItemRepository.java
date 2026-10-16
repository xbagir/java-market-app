package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import ru.yandex.practicum.mymarket.model.OrderItem;

import java.util.Collection;

import reactor.core.publisher.Flux;

public interface OrderItemRepository extends R2dbcRepository<OrderItem, Long> {

    @Query("SELECT * FROM order_items WHERE order_id = :orderId")
    Flux<OrderItem> findByOrderId(long orderId);

    @Query("SELECT * FROM order_items WHERE order_id IN (:orderId)")
    Flux<OrderItem> findByOrderIdIn(Collection<Long> orderId);
}
