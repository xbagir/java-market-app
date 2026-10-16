package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.Paging;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import reactor.core.publisher.Mono;

@Controller
@Validated
public class MarketController {

    private static final int GRID_COLUMNS = 3;
    private static final ItemDto EMPTY_CELL = new ItemDto(-1, "", "", "", 0, 0);

    private final ItemService itemService;
    private final CartService cartService;

    public MarketController(ItemService itemService, CartService cartService) {
        this.itemService = itemService;
        this.cartService = cartService;
    }

    @GetMapping({"/", "/items"})
    public Mono<String> items(@RequestParam(name = "search", defaultValue = "") String search,
                              @RequestParam(name = "sort", defaultValue = "NO") SortOption sort,
                              @RequestParam(name = "pageNumber", defaultValue = "1") @Min(1) int pageNumber,
                              @RequestParam(name = "pageSize", defaultValue = "5") @Min(2) @Max(100) int pageSize,
                              Model model) {
        return itemService.findPage(search, sort, pageNumber, pageSize)
                .zipWith(cartService.quantitiesByItemIds())
                .doOnNext(tuple -> {
                    var page = tuple.getT1();
                    var quantities = tuple.getT2();
                    List<ItemDto> items = page.getContent().stream()
                            .map(dto -> dto.withCount(quantities.getOrDefault(dto.id(), 0)))
                            .toList();
                    model.addAttribute("items", toGrid(items));
                    model.addAttribute("search", search);
                    model.addAttribute("sort", sort.name());
                    model.addAttribute("paging", new Paging(pageSize, page.getNumber() + 1,
                            page.hasPrevious(), page.hasNext()));
                })
                .thenReturn("items");
    }

    @GetMapping("/items/{id}")
    public Mono<String> item(@PathVariable long id, Model model) {
        return itemService.findById(id)
                .zipWith(cartService.getQuantityByItemId(id))
                .doOnNext(tuple -> model.addAttribute("item",
                        tuple.getT1().withCount(tuple.getT2())))
                .thenReturn("item");
    }

    private List<List<ItemDto>> toGrid(List<ItemDto> items) {
        List<List<ItemDto>> grid = new ArrayList<>();
        for (int i = 0; i < items.size(); i += GRID_COLUMNS) {
            List<ItemDto> row = new ArrayList<>(GRID_COLUMNS);
            for (int j = i; j < i + GRID_COLUMNS && j < items.size(); j++) {
                row.add(items.get(j));
            }
            while (row.size() < GRID_COLUMNS) {
                row.add(EMPTY_CELL);
            }
            grid.add(row);
        }
        return grid;
    }
}
