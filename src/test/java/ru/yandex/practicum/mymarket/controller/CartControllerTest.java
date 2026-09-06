package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    private CartItem cartItem(long id, Item item, int quantity) {
        CartItem cartItem = new CartItem(item, quantity);
        ReflectionTestUtils.setField(cartItem, "id", id);
        return cartItem;
    }

    private Item item(long id, String title, long price) {
        Item item = new Item(title, "Описание " + title, "images/ball.svg", price);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    @Test
    void cartPageRendersItemsAndTotal() throws Exception {
        Item ball = item(1L, "Мяч", 1490);
        when(cartService.getCartItems()).thenReturn(List.of(cartItem(1L, ball, 2)));
        when(cartService.getTotal()).thenReturn(2980L);

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items", "total"));
    }

    @Test
    void cartAliasPathWorksToo() throws Exception {
        when(cartService.getCartItems()).thenReturn(List.of());
        when(cartService.getTotal()).thenReturn(0L);

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));
    }

    @Test
    void postCartItemUpdatesQuantityAndRendersCart() throws Exception {
        when(cartService.getCartItems()).thenReturn(List.of());
        when(cartService.getTotal()).thenReturn(0L);

        mockMvc.perform(post("/cart/items").param("id", "1").param("action", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));

        verify(cartService).update(1L, Action.DELETE);
    }
}