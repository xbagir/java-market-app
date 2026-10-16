package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Mono;

@WebFluxTest(MarketController.class)
class MarketControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    private ItemDto item(long id, String title, long price) {
        return new ItemDto(id, title, "Описание " + title, "images/ball.svg", price, 0);
    }

    @Test
    void itemsPageRendersGridWithDefaultParams() {
        var page = new PageImpl<>(List.of(item(1L, "Мяч", 1490), item(2L, "Кукла", 1890)),
                PageRequest.of(0, 5), 2);
        when(itemService.findPage("", SortOption.NO, 1, 5)).thenReturn(Mono.just(page));
        when(cartService.quantitiesByItemIds()).thenReturn(Mono.just(Map.of()));

        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч"));
    }

    @Test
    void rootPathShowsSamePage() {
        var page = new PageImpl<>(List.of(item(1L, "Мяч", 1490)), PageRequest.of(0, 5), 1);
        when(itemService.findPage("", SortOption.NO, 1, 5)).thenReturn(Mono.just(page));
        when(cartService.quantitiesByItemIds()).thenReturn(Mono.just(Map.of()));

        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч"));
    }

    @Test
    void itemsPagePassesSearchSortAndPaging() {
        var page = new PageImpl<>(List.of(item(1L, "Мяч", 1490)), PageRequest.of(1, 10), 1);
        when(itemService.findPage("мяч", SortOption.ALPHA, 2, 10)).thenReturn(Mono.just(page));
        when(cartService.quantitiesByItemIds()).thenReturn(Mono.just(Map.of()));

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("search", "мяч")
                        .queryParam("sort", "ALPHA")
                        .queryParam("pageNumber", "2")
                        .queryParam("pageSize", "10")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч"));
    }

    @Test
    void itemsPageAcceptsMaxPageSize() {
        var page = new PageImpl<>(List.of(item(1L, "Мяч", 1490)), PageRequest.of(0, 100), 1);
        when(itemService.findPage("", SortOption.NO, 1, 100)).thenReturn(Mono.just(page));
        when(cartService.quantitiesByItemIds()).thenReturn(Mono.just(Map.of()));

        webTestClient.get().uri("/items?pageSize=100")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void itemsPageRejectsPageSizeAboveMax() {
        webTestClient.get().uri("/items?pageSize=101")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void itemsPageRejectsPageNumberBelowMin() {
        webTestClient.get().uri("/items?pageNumber=0")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void itemPageRendersSingleItem() {
        when(itemService.findById(1L)).thenReturn(Mono.just(item(1L, "Мяч", 1490)));
        when(cartService.getQuantityByItemId(1L)).thenReturn(Mono.just(2));

        webTestClient.get().uri("/items/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч"));
    }

    @Test
    void itemPageReturns404WhenMissing() {
        when(itemService.findById(999L)).thenReturn(Mono.error(new NotFoundException("нет")));
        when(cartService.getQuantityByItemId(999L)).thenReturn(Mono.just(0));

        webTestClient.get().uri("/items/999")
                .exchange()
                .expectStatus().isNotFound();
    }
}
