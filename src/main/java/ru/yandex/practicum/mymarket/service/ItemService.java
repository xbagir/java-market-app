package ru.yandex.practicum.mymarket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import reactor.core.publisher.Mono;

import java.util.Locale;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Mono<Page<ItemDto>> findPage(String search, SortOption sort, int pageNumber, int pageSize) {
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + query + "%";
        Pageable pageable = PageRequest.of(Math.max(pageNumber, 1) - 1, pageSize, sortOf(sort));
        return itemRepository.searchByPattern(pattern, pageable)
                .map(item -> ItemDto.of(item, 0))
                .collectList()
                .zipWith(itemRepository.countByPattern(pattern))
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }

    public Mono<ItemDto> findById(long id) {
        return itemRepository.findById(id)
                .map(item -> ItemDto.of(item, 0))
                .switchIfEmpty(Mono.error(new NotFoundException("Товар с id " + id + " не найден")));
    }

    private Sort sortOf(SortOption sort) {
        return switch (sort) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            case NO -> Sort.by("id").ascending();
        };
    }
}
