package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemRepository);
    }

    private Item item(long id, String title, long price) {
        Item item = new Item(title, "Описание " + title, "images/ball.svg", price);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    @Test
    void findPageWithoutSearchMatchesAll() {
        when(itemRepository.searchOrderById("%%", 5, 0))
                .thenReturn(Flux.just(item(1L, "Мяч", 1490)));
        when(itemRepository.countByPattern("%%")).thenReturn(Mono.just(1L));

        StepVerifier.create(itemService.findPage("", SortOption.NO, 1, 5))
                .assertNext(page -> {
                    assertThat(page.getContent()).hasSize(1);
                    assertThat(page.getContent().get(0).id()).isEqualTo(1L);
                    assertThat(page.getTotalElements()).isEqualTo(1L);
                })
                .verifyComplete();

        verify(itemRepository).searchOrderById("%%", 5, 0);
    }

    @Test
    void findPageWithSearchBuildsLikePattern() {
        when(itemRepository.searchOrderByTitle("%мяч%", 5, 0)).thenReturn(Flux.empty());
        when(itemRepository.countByPattern("%мяч%")).thenReturn(Mono.just(0L));

        StepVerifier.create(itemService.findPage("мяч", SortOption.ALPHA, 1, 5))
                .assertNext(page -> assertThat(page.getContent()).isEmpty())
                .verifyComplete();

        verify(itemRepository).searchOrderByTitle("%мяч%", 5, 0);
    }

    @Test
    void priceSortUsesPriceOrdering() {
        when(itemRepository.searchOrderByPrice("%%", 5, 0)).thenReturn(Flux.empty());
        when(itemRepository.countByPattern("%%")).thenReturn(Mono.just(0L));

        StepVerifier.create(itemService.findPage("", SortOption.PRICE, 1, 5))
                .expectNextCount(1)
                .verifyComplete();

        verify(itemRepository).searchOrderByPrice("%%", 5, 0);
    }

    @Test
    void pageNumberIsConvertedToOffset() {
        when(itemRepository.searchOrderById("%%", 5, 5)).thenReturn(Flux.empty());
        when(itemRepository.countByPattern("%%")).thenReturn(Mono.just(0L));

        StepVerifier.create(itemService.findPage("", SortOption.NO, 2, 5))
                .expectNextCount(1)
                .verifyComplete();

        verify(itemRepository).searchOrderById("%%", 5, 5);
    }

    @Test
    void findByIdReturnsDto() {
        Item item = item(1L, "Мяч", 1490);
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));

        StepVerifier.create(itemService.findById(1L))
                .assertNext(dto -> assertThat(dto).isEqualTo(ItemDto.of(item, 0)))
                .verifyComplete();
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(itemRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(itemService.findById(999L))
                .expectError(NotFoundException.class)
                .verify();
    }
}
