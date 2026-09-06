package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void findPageWithoutSearchUsesFindAll() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("id").ascending());
        Page<Item> page = new PageImpl<>(List.of(item(1L, "Мяч", 1490)), pageable, 1);
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Item> result = itemService.findPage("", SortOption.NO, 1, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("id").ascending());
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findPageWithSearchUsesDerivedQuery() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("title").ascending());
        when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                any(String.class), any(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        itemService.findPage("мяч", SortOption.ALPHA, 1, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                eq("мяч"), eq("мяч"), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("title").ascending());
    }

    @Test
    void priceSortUsesPriceOrdering() {
        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 5), 0));

        itemService.findPage("", SortOption.PRICE, 1, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("price").ascending());
    }

    @Test
    void pageNumberIsConvertedFromOneBasedToZeroBased() {
        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        itemService.findPage("", SortOption.NO, 2, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    void findByIdReturnsItem() {
        Item item = item(1L, "Мяч", 1490);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThat(itemService.findById(1L)).isEqualTo(item);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.findById(999L))
                .isInstanceOf(NotFoundException.class);
    }
}