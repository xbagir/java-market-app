package ru.yandex.practicum.mymarket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import reactor.core.publisher.Flux;
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
        int page = Math.max(pageNumber, 1) - 1;
        Pageable pageable = PageRequest.of(page, pageSize);
        long offset = (long) page * pageSize;
        Flux<Item> content = switch (sort) {
            case ALPHA -> itemRepository.searchOrderByTitle(pattern, pageSize, offset);
            case PRICE -> itemRepository.searchOrderByPrice(pattern, pageSize, offset);
            case NO -> itemRepository.searchOrderById(pattern, pageSize, offset);
        };
        return content
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
}
