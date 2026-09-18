package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import ru.yandex.practicum.mymarket.model.Order;

import reactor.core.publisher.Flux;

public interface OrderRepository extends R2dbcRepository<Order, Long> {

    @Query("SELECT * FROM shop_orders ORDER BY id DESC")
    Flux<Order> findAllByOrderByIdDesc();
}
