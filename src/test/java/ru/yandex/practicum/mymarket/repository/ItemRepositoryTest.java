package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import ru.yandex.practicum.mymarket.model.Item;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DataR2dbcTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    private Mono<Item> item(String title, String description, long price) {
        return itemRepository.save(new Item(title, description, "images/ball.svg", price));
    }

    @Test
    void searchFindsByTitleIgnoringCase() {
        Mono<Item> setup = item("Мяч футбольный", "Для игры", 1490)
                .then(item("Кукла", "Нарядная", 1890));

        StepVerifier.create(setup.thenMany(
                        itemRepository.searchByPattern("%мяч%", PageRequest.of(0, 10))))
                .map(Item::getTitle)
                .expectNext("Мяч футбольный")
                .verifyComplete();
    }

    @Test
    void searchFindsByDescription() {
        Mono<Item> setup = item("Самосвал", "Инерционная машинка с кузовом", 1190)
                .then(item("Кубики", "Для малышей", 690));

        StepVerifier.create(setup.thenMany(
                        itemRepository.searchByPattern("%кузовом%", PageRequest.of(0, 10))))
                .map(Item::getTitle)
                .expectNext("Самосвал")
                .verifyComplete();
    }

    @Test
    void searchReturnsEmptyWhenNoMatch() {
        StepVerifier.create(item("Мяч", "Круглый", 1490)
                        .thenMany(itemRepository.searchByPattern("%несуществующее%", PageRequest.of(0, 10))))
                .verifyComplete();
    }

    @Test
    void pagingReturnsRequestedSlice() {
        Mono<Item> setup = item("Товар 0", "Описание 0", 100)
                .then(item("Товар 1", "Описание 1", 101))
                .then(item("Товар 2", "Описание 2", 102))
                .then(item("Товар 3", "Описание 3", 103))
                .then(item("Товар 4", "Описание 4", 104))
                .then(item("Товар 5", "Описание 5", 105));

        StepVerifier.create(setup.thenMany(
                        itemRepository.searchByPattern("%%", PageRequest.of(0, 5, Sort.by("id").ascending()))))
                .expectNextCount(5)
                .verifyComplete();

        StepVerifier.create(itemRepository.countByPattern("%%"))
                .expectNext(6L)
                .verifyComplete();
    }

    @Test
    void sortByPriceOrdersAscending() {
        Mono<Item> setup = item("Кубики", "Дешёвые", 690)
                .then(item("Робот", "Дорогой", 3490))
                .then(item("Мяч", "Средний", 1490));

        StepVerifier.create(setup.thenMany(itemRepository.findAll(Sort.by("price").ascending())))
                .map(Item::getPrice)
                .expectNext(690L, 1490L, 3490L)
                .verifyComplete();
    }
}
