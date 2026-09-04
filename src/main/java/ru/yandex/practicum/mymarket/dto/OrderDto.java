package ru.yandex.practicum.mymarket.dto;

import ru.yandex.practicum.mymarket.model.Order;

import java.util.List;

public record OrderDto(long id, List<ItemDto> items, long totalSum) {

    public static OrderDto of(Order order) {
        List<ItemDto> items = order.getItems().stream()
                .map(oi -> new ItemDto(oi.getItem().getId(), oi.getItem().getTitle(),
                        oi.getItem().getDescription(), oi.getItem().getImgPath(),
                        oi.getPrice(), oi.getQuantity()))
                .toList();
        return new OrderDto(order.getId(), items, order.getTotalSum());
    }
}