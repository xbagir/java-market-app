package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import ru.yandex.practicum.mymarket.model.Item;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    private Item item(String title, String description, long price) {
        return itemRepository.save(new Item(title, description, "images/ball.svg", price));
    }

    @Test
    void searchFindsByTitleIgnoringCase() {
        item("Мяч футбольный", "Для игры", 1490);
        item("Кукла", "Нарядная", 1890);

        Page<Item> result = itemRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        "мяч", "мяч", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Item::getTitle)
                .containsExactly("Мяч футбольный");
    }

    @Test
    void searchFindsByDescription() {
        item("Самосвал", "Инерционная машинка с кузовом", 1190);
        item("Кубики", "Для малышей", 690);

        Page<Item> result = itemRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        "кузовом", "кузовом", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Item::getTitle)
                .containsExactly("Самосвал");
    }

    @Test
    void searchReturnsEmptyWhenNoMatch() {
        item("Мяч", "Круглый", 1490);

        Page<Item> result = itemRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        "несуществующее", "несуществующее", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void pagingReturnsRequestedSlice() {
        for (int i = 0; i < 6; i++) {
            item("Товар " + i, "Описание " + i, 100 + i);
        }

        Page<Item> page = itemRepository.findAll(PageRequest.of(0, 5, Sort.by("id").ascending()));

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void sortByPriceOrdersAscending() {
        item("Кубики", "Дешёвые", 690);
        item("Робот", "Дорогой", 3490);
        item("Мяч", "Средний", 1490);

        List<Item> sorted = itemRepository.findAll(Sort.by("price").ascending());

        assertThat(sorted).extracting(Item::getPrice).containsExactly(690L, 1490L, 3490L);
    }
}