package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.CartView;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Mono;

@WebFluxTest(CartController.class)
class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    private ItemDto item(long id, String title, long price, int count) {
        return new ItemDto(id, title, "Описание " + title, "images/ball.svg", price, count);
    }

    @Test
    void cartPageRendersItemsAndTotal() {
        when(cartService.getCartView()).thenReturn(Mono.just(new CartView(
                List.of(item(1L, "Мяч", 1490, 2)), 2L * 1490)));

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч"));
    }

    @Test
    void cartAliasPathWorksToo() {
        when(cartService.getCartView()).thenReturn(Mono.just(new CartView(List.of(), 0L)));

        webTestClient.get().uri("/cart")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void postCartItemUpdatesQuantityAndRedirects() {
        when(cartService.update(1L, Action.DELETE)).thenReturn(Mono.empty());

        webTestClient.post().uri("/cart/items?id=1&action=DELETE")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/cart/items");

        verify(cartService).update(1L, Action.DELETE);
    }

    @Test
    void postCatalogItemUpdatesCartAndRedirectsBack() {
        when(cartService.update(1L, Action.PLUS)).thenReturn(Mono.empty());

        webTestClient.post().uri("/items?id=1&action=PLUS&search=&sort=NO&pageNumber=1&pageSize=5")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items?search=&sort=NO&pageNumber=1&pageSize=5");

        verify(cartService).update(1L, Action.PLUS);
    }

    @Test
    void postCatalogItemRejectsInvalidPageSize() {
        webTestClient.post().uri("/items?id=1&action=PLUS&search=&sort=NO&pageNumber=1&pageSize=1")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void postItemPageActionRedirectsToItem() {
        when(cartService.update(1L, Action.MINUS)).thenReturn(Mono.empty());

        webTestClient.post().uri("/items/1?action=MINUS")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items/1");

        verify(cartService).update(1L, Action.MINUS);
    }
}
