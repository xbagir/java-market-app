package ru.yandex.practicum.mymarket.controller;

import org.springframework.data.domain.Page;
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
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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
    public String items(@RequestParam(name = "search", defaultValue = "") String search,
                        @RequestParam(name = "sort", defaultValue = "NO") SortOption sort,
                        @RequestParam(name = "pageNumber", defaultValue = "1") @Min(1) int pageNumber,
                        @RequestParam(name = "pageSize", defaultValue = "5") @Min(2) @Max(50) int pageSize,
                        Model model) {
        Page<ItemDto> page = itemService.findPage(search, sort, pageNumber, pageSize);
        Map<Long, Integer> quantities = cartService.quantitiesByItemIds(
                page.getContent().stream().map(ItemDto::id).toList());
        List<ItemDto> items = page.getContent().stream()
                .map(dto -> dto.withCount(quantities.getOrDefault(dto.id(), 0)))
                .toList();
        model.addAttribute("items", toGrid(items));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort.name());
        model.addAttribute("paging", new Paging(pageSize, page.getNumber() + 1,
                page.hasPrevious(), page.hasNext()));
        return "items";
    }

    @GetMapping("/items/{id}")
    public String item(@PathVariable long id, Model model) {
        ItemDto item = itemService.findById(id)
                .withCount(cartService.getQuantityByItemId(id));
        model.addAttribute("item", item);
        return "item";
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
