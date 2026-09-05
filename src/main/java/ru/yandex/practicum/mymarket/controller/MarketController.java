package ru.yandex.practicum.mymarket.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.Paging;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
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
                        @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
                        @RequestParam(name = "pageSize", defaultValue = "5") int pageSize,
                        Model model) {
        Page<Item> page = itemService.findPage(search, sort, pageNumber, pageSize);
        Map<Long, Integer> quantities = cartService.quantitiesByItemId();
        model.addAttribute("items", toGrid(page.getContent(), quantities));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort.name());
        model.addAttribute("paging", new Paging(pageSize, page.getNumber() + 1,
                page.hasPrevious(), page.hasNext()));
        return "items";
    }

    @GetMapping("/items/{id}")
    public String item(@PathVariable long id, Model model) {
        model.addAttribute("item", toDto(itemService.findById(id),
                cartService.quantitiesByItemId().getOrDefault(id, 0)));
        return "item";
    }

    @PostMapping("/items")
    public String updateItemQuantity(@RequestParam long id,
                                     @RequestParam Action action,
                                     @RequestParam(name = "search", defaultValue = "") String search,
                                     @RequestParam(name = "sort", defaultValue = "NO") SortOption sort,
                                     @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
                                     @RequestParam(name = "pageSize", defaultValue = "5") int pageSize,
                                     RedirectAttributes redirectAttributes) {
        cartService.update(id, action);
        redirectAttributes.addAttribute("search", search);
        redirectAttributes.addAttribute("sort", sort.name());
        redirectAttributes.addAttribute("pageNumber", pageNumber);
        redirectAttributes.addAttribute("pageSize", pageSize);
        return "redirect:/items";
    }

    @PostMapping("/items/{id}")
    public String updateItem(@PathVariable long id, @RequestParam Action action, Model model) {
        cartService.update(id, action);
        model.addAttribute("item", toDto(itemService.findById(id),
                cartService.quantitiesByItemId().getOrDefault(id, 0)));
        return "item";
    }

    private ItemDto toDto(Item item, int count) {
        return new ItemDto(item.getId(), item.getTitle(), item.getDescription(),
                item.getImgPath(), item.getPrice(), count);
    }

    private List<List<ItemDto>> toGrid(List<Item> items, Map<Long, Integer> quantities) {
        List<List<ItemDto>> grid = new ArrayList<>();
        for (int i = 0; i < items.size(); i += GRID_COLUMNS) {
            List<ItemDto> row = new ArrayList<>(GRID_COLUMNS);
            for (int j = i; j < i + GRID_COLUMNS && j < items.size(); j++) {
                row.add(toDto(items.get(j), quantities.getOrDefault(items.get(j).getId(), 0)));
            }
            while (row.size() < GRID_COLUMNS) {
                row.add(EMPTY_CELL);
            }
            grid.add(row);
        }
        return grid;
    }
}