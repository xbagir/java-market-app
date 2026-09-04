package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.yandex.practicum.mymarket.model.Order;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByOrderByIdDesc();
}