package ru.yandex.practicum.mymarket.dto;

import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;

import java.util.List;
import java.util.Map;

public record OrderDto(long id, List<ItemDto> items, long totalSum) {

    public static OrderDto of(Order order, List<OrderItem> orderItems, Map<Long, Item> itemsById) {
        List<ItemDto> items = orderItems.stream()
                .map(oi -> {
                    Item item = itemsById.get(oi.getItemId());
                    return new ItemDto(item.getId(), item.getTitle(), item.getDescription(),
                            item.getImgPath(), oi.getPrice(), oi.getQuantity());
                })
                .toList();
        return new OrderDto(order.getId(), items, order.getTotalSum());
    }
}
