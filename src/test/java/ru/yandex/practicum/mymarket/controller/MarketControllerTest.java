package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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

    private Item item(long id, String title, long price) {
        Item item = new Item(title, "Описание " + title, "images/ball.svg", price);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private Page<Item> pageOf(List<Item> items, int pageNumber, int pageSize) {
        return new PageImpl<>(items, PageRequest.of(pageNumber, pageSize), items.size());
    }

    @Test
    void itemsPageRendersGridWithDefaultParams() throws Exception {
        Page<Item> page = pageOf(List.of(item(1L, "Мяч", 1490), item(2L, "Кукла", 1890)), 0, 5);
        when(itemService.findPage("", SortOption.NO, 1, 5)).thenReturn(page);
        when(cartService.quantitiesByItemId()).thenReturn(Map.of());

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items", "search", "sort", "paging"));
    }

    @Test
    void rootPathShowsSamePage() throws Exception {
        Page<Item> page = pageOf(List.of(item(1L, "Мяч", 1490)), 0, 5);
        when(itemService.findPage("", SortOption.NO, 1, 5)).thenReturn(page);
        when(cartService.quantitiesByItemId()).thenReturn(Map.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"));
    }

    @Test
    void itemsPagePassesSearchSortAndPaging() throws Exception {
        Page<Item> page = pageOf(List.of(item(1L, "Мяч", 1490)), 1, 10);
        when(itemService.findPage("мяч", SortOption.ALPHA, 2, 10)).thenReturn(page);
        when(cartService.quantitiesByItemId()).thenReturn(Map.of());

        mockMvc.perform(get("/items").param("search", "мяч").param("sort", "ALPHA")
                        .param("pageNumber", "2").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("search", "мяч"))
                .andExpect(model().attribute("sort", "ALPHA"));
    }

    @Test
    void itemPageRendersSingleItem() throws Exception {
        Item ball = item(1L, "Мяч", 1490);
        when(itemService.findById(1L)).thenReturn(ball);
        when(cartService.quantitiesByItemId()).thenReturn(Map.of(1L, 2));

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

    @Test
    void postItemsUpdatesCartAndRedirectsBack() throws Exception {
        mockMvc.perform(post("/items").param("id", "1").param("action", "PLUS")
                        .param("search", "").param("sort", "NO")
                        .param("pageNumber", "1").param("pageSize", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=&sort=NO&pageNumber=1&pageSize=5"));

        verify(cartService).update(1L, Action.PLUS);
    }

    @Test
    void postItemUpdatesCartAndRendersItemPage() throws Exception {
        Item ball = item(1L, "Мяч", 1490);
        when(itemService.findById(1L)).thenReturn(ball);
        when(cartService.quantitiesByItemId()).thenReturn(Map.of(1L, 1));

        mockMvc.perform(post("/items/1").param("action", "MINUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"));

        verify(cartService).update(1L, Action.MINUS);
    }
}