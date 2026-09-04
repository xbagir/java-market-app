package ru.yandex.practicum.mymarket.dto;

/**
 * Параметры пагинации витрины (номера страниц — с единицы).
 */
public record Paging(int pageSize, int pageNumber, boolean hasPrevious, boolean hasNext) {
}