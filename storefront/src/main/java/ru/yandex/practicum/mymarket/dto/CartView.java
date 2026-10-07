package ru.yandex.practicum.mymarket.dto;

import java.util.List;

public record CartView(List<ItemDto> items, long total) {
}