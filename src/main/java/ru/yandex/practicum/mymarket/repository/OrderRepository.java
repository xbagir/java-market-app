package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.yandex.practicum.mymarket.model.Order;

import java.util.List;

public interface OrderRepository extends ReadOnlyRepository<Order, Long>, JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.item"})
    List<Order> findAllByOrderByIdDesc();
}
