package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;

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
        Mono<Item> setup = itemRepository.deleteAll()
                .then(item("Мяч футбольный", "Для игры", 1490))
                .then(item("Кукла", "Нарядная", 1890));

        StepVerifier.create(setup.thenMany(
                        itemRepository.searchOrderById("%мяч%", 10, 0)))
                .expectNextMatches(item -> item.getTitle().equals("Мяч футбольный"))
                .verifyComplete();
    }

    @Test
    void searchFindsByDescription() {
        Mono<Item> setup = itemRepository.deleteAll()
                .then(item("Самосвал", "Инерционная машинка с кузовом", 1190))
                .then(item("Кубики", "Для малышей", 690));

        StepVerifier.create(setup.thenMany(
                        itemRepository.searchOrderById("%кузовом%", 10, 0)))
                .expectNextMatches(item -> item.getTitle().equals("Самосвал"))
                .verifyComplete();
    }

    @Test
    void searchReturnsEmptyWhenNoMatch() {
        StepVerifier.create(itemRepository.deleteAll()
                        .then(item("Мяч", "Круглый", 1490))
                        .thenMany(itemRepository.searchOrderById("%несуществующее%", 10, 0)))
                .verifyComplete();
    }

    @Test
    void pagingReturnsRequestedSlice() {
        Mono<Item> setup = itemRepository.deleteAll()
                .then(item("Товар 0", "Описание 0", 100))
                .then(item("Товар 1", "Описание 1", 101))
                .then(item("Товар 2", "Описание 2", 102))
                .then(item("Товар 3", "Описание 3", 103))
                .then(item("Товар 4", "Описание 4", 104))
                .then(item("Товар 5", "Описание 5", 105));

        StepVerifier.create(setup.thenMany(
                        itemRepository.searchOrderById("%%", 5, 0)))
                .expectNextCount(5)
                .verifyComplete();

        StepVerifier.create(itemRepository.countByPattern("%%"))
                .expectNext(6L)
                .verifyComplete();
    }

    @Test
    void sortByPriceOrdersAscending() {
        Mono<Item> setup = itemRepository.deleteAll()
                .then(item("Кубики", "Дешёвые", 690))
                .then(item("Робот", "Дорогой", 3490))
                .then(item("Мяч", "Средний", 1490));

        StepVerifier.create(setup.thenMany(
                        itemRepository.searchOrderByPrice("%%", 10, 0)))
                .expectNextMatches(item -> item.getPrice() == 690L)
                .expectNextMatches(item -> item.getPrice() == 1490L)
                .expectNextMatches(item -> item.getPrice() == 3490L)
                .verifyComplete();
    }

    @Test
    void sortByTitleOrdersAlphabetically() {
        Mono<Item> setup = itemRepository.deleteAll()
                .then(item("Кубики", "Для малышей", 690))
                .then(item("Робот", "Дорогой", 3490))
                .then(item("Мяч", "Средний", 1490));

        StepVerifier.create(setup.thenMany(
                        itemRepository.searchOrderByTitle("%%", 10, 0)))
                .expectNextMatches(item -> item.getTitle().equals("Кубики"))
                .expectNextMatches(item -> item.getTitle().equals("Мяч"))
                .expectNextMatches(item -> item.getTitle().equals("Робот"))
                .verifyComplete();
    }
}
