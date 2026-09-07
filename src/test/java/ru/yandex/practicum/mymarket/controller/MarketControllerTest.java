package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MarketController.class)
class MarketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    private ItemDto item(long id, String title, long price) {
        return new ItemDto(id, title, "Описание " + title, "images/ball.svg", price, 0);
    }

    private Page<ItemDto> pageOf(List<ItemDto> items, int pageNumber, int pageSize) {
        return new PageImpl<>(items, PageRequest.of(pageNumber, pageSize), items.size());
    }

    @Test
    void itemsPageRendersGridWithDefaultParams() throws Exception {
        Page<ItemDto> page = pageOf(List.of(item(1L, "Мяч", 1490), item(2L, "Кукла", 1890)), 0, 5);
        when(itemService.findPage("", SortOption.NO, 1, 5)).thenReturn(page);
        when(cartService.quantitiesByItemIds(anyList())).thenReturn(Map.of());

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items", "search", "sort", "paging"));
    }

    @Test
    void rootPathShowsSamePage() throws Exception {
        Page<ItemDto> page = pageOf(List.of(item(1L, "Мяч", 1490)), 0, 5);
        when(itemService.findPage("", SortOption.NO, 1, 5)).thenReturn(page);
        when(cartService.quantitiesByItemIds(anyList())).thenReturn(Map.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"));
    }

    @Test
    void itemsPagePassesSearchSortAndPaging() throws Exception {
        Page<ItemDto> page = pageOf(List.of(item(1L, "Мяч", 1490)), 1, 10);
        when(itemService.findPage("мяч", SortOption.ALPHA, 2, 10)).thenReturn(page);
        when(cartService.quantitiesByItemIds(anyList())).thenReturn(Map.of());

        mockMvc.perform(get("/items").param("search", "мяч").param("sort", "ALPHA")
                        .param("pageNumber", "2").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("search", "мяч"))
                .andExpect(model().attribute("sort", "ALPHA"));
    }

    @Test
    void itemPageRendersSingleItem() throws Exception {
        ItemDto ball = item(1L, "Мяч", 1490);
        when(itemService.findById(1L)).thenReturn(ball);
        when(cartService.getQuantityByItemId(1L)).thenReturn(2);

        mockMvc.perform(get("/items/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"));
    }

    @Test
    void itemPageReturns404WhenMissing() throws Exception {
        when(itemService.findById(999L)).thenThrow(new NotFoundException("нет"));

        mockMvc.perform(get("/items/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }
}