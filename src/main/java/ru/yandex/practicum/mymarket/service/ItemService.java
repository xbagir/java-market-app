package ru.yandex.practicum.mymarket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@Service
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Page<ItemDto> findPage(String search, SortOption sort, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(pageNumber, 1) - 1, pageSize, sortOf(sort));
        Page<Item> page;
        if (search == null || search.isBlank()) {
            page = itemRepository.findAll(pageable);
        } else {
            page = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search, search, pageable);
        }
        return page.map(item -> ItemDto.of(item, 0));
    }

    public ItemDto findById(long id) {
        return itemRepository.findById(id)
                .map(item -> ItemDto.of(item, 0))
                .orElseThrow(() -> new NotFoundException("Товар с id " + id + " не найден"));
    }

    private Sort sortOf(SortOption sort) {
        return switch (sort) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            case NO -> Sort.by("id").ascending();
        };
    }
}
