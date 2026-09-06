package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void ordersPageRendersList() throws Exception {
        when(orderService.getOrders()).thenReturn(List.of(new OrderDto(1L, List.of(), 1000)));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    void orderPageRendersWithNewOrderFlag() throws Exception {
        when(orderService.getOrder(1L)).thenReturn(new OrderDto(1L, List.of(), 1000));

        mockMvc.perform(get("/orders/1").param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("newOrder", true));
    }

    @Test
    void orderPageDefaultsNewOrderToFalse() throws Exception {
        when(orderService.getOrder(1L)).thenReturn(new OrderDto(1L, List.of(), 1000));

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("newOrder", false));
    }

    @Test
    void orderPageReturns404WhenMissing() throws Exception {
        when(orderService.getOrder(999L)).thenThrow(new NotFoundException("нет"));

        mockMvc.perform(get("/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void buyRedirectsToCreatedOrder() throws Exception {
        when(orderService.createOrderFromCart()).thenReturn(new OrderDto(42L, List.of(), 5000));

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/42?newOrder=true"));
    }
}