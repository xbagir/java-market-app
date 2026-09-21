package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.CartView;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    private ItemDto item(long id, String title, long price, int count) {
        return new ItemDto(id, title, "Описание " + title, "images/ball.svg", price, count);
    }

    @Test
    void cartPageRendersItemsAndTotal() throws Exception {
        when(cartService.getCartView()).thenReturn(new CartView(
                List.of(item(1L, "Мяч", 1490, 2)), 2L * 1490));

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items", "total"));
    }

    @Test
    void cartAliasPathWorksToo() throws Exception {
        when(cartService.getCartView()).thenReturn(new CartView(List.of(), 0L));

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));
    }

    @Test
    void postCartItemUpdatesQuantityAndRedirects() throws Exception {
        mockMvc.perform(post("/cart/items").param("id", "1").param("action", "DELETE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verify(cartService).update(1L, Action.DELETE);
    }

    @Test
    void postCatalogItemUpdatesCartAndRedirectsBack() throws Exception {
        mockMvc.perform(post("/items").param("id", "1").param("action", "PLUS")
                        .param("search", "").param("sort", "NO")
                        .param("pageNumber", "1").param("pageSize", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=&sort=NO&pageNumber=1&pageSize=5"));

        verify(cartService).update(1L, Action.PLUS);
    }

    @Test
    void postItemPageActionRedirectsToItem() throws Exception {
        mockMvc.perform(post("/items/1").param("action", "MINUS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/1"));

        verify(cartService).update(1L, Action.MINUS);
    }
}