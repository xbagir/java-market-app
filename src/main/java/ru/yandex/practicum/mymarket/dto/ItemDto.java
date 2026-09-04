package ru.yandex.practicum.mymarket.dto;

/**
 * Объект товара для отображения в шаблонах.
 * {@code count} — количество товара в корзине (0 — не в корзине).
 */
public record ItemDto(long id, String title, String description, String imgPath, long price, int count) {

    public static ItemDto of(ru.yandex.practicum.mymarket.model.Item item, int count) {
        return new ItemDto(item.getId(), item.getTitle(), item.getDescription(),
                item.getImgPath(), item.getPrice(), count);
    }
}