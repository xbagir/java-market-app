package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Mono;

@WebFluxTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @Test
    void ordersPageRendersList() {
        when(orderService.getOrders()).thenReturn(Mono.just(List.of(new OrderDto(1L, List.of(), 1000))));

        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Заказ"));
    }

    @Test
    void orderPageRendersWithNewOrderFlag() {
        when(orderService.getOrder(1L)).thenReturn(Mono.just(new OrderDto(1L, List.of(), 1000)));

        webTestClient.get().uri("/orders/1?newOrder=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Поздравляем"));
    }

    @Test
    void orderPageDefaultsNewOrderToFalse() {
        when(orderService.getOrder(1L)).thenReturn(Mono.just(new OrderDto(1L, List.of(), 1000)));

        webTestClient.get().uri("/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("1000"));
    }

    @Test
    void orderPageReturns404WhenMissing() {
        when(orderService.getOrder(999L)).thenReturn(Mono.error(new NotFoundException("нет")));

        webTestClient.get().uri("/orders/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void buyRedirectsToCreatedOrder() {
        when(orderService.createOrderFromCart()).thenReturn(Mono.just(new OrderDto(42L, List.of(), 5000)));

        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/orders/42?newOrder=true");
    }

    @Test
    void buyEmptyCartReturns400() {
        when(orderService.createOrderFromCart()).thenReturn(Mono.error(new EmptyCartException("пусто")));

        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
