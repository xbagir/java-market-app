package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.practicum.mymarket.config.SecurityConfig;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.UserPrincipal;
import ru.yandex.practicum.mymarket.security.CurrentUserProvider;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.UserService;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Mono;

@WebFluxTest(MarketController.class)
@Import(SecurityConfig.class)
class MarketControllerTest {

    private static final long USER_ID = 7L;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private ItemDto item(long id, String title, long price) {
        return new ItemDto(id, title, "Описание " + title, "images/ball.svg", price, 0);
    }

    private void anonymous() {
        lenient().when(currentUserProvider.get()).thenReturn(Mono.empty());
        lenient().when(currentUserProvider.getUserId()).thenReturn(Mono.empty());
    }

    private void authenticated() {
        UserPrincipal principal = new UserPrincipal(USER_ID, "buyer", "secret", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        lenient().when(currentUserProvider.get()).thenReturn(Mono.just(principal));
        lenient().when(currentUserProvider.getUserId()).thenReturn(Mono.just(USER_ID));
    }

    @Test
    void itemsPageRendersGridWithDefaultParams() {
        anonymous();
        var page = new PageImpl<>(List.of(item(1L, "Мяч", 1490), item(2L, "Кукла", 1890)),
                PageRequest.of(0, 5), 2);
        when(itemService.findPage("", SortOption.NO, 1, 5)).thenReturn(Mono.just(page));

        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч"));
    }

    @Test
    void rootPathShowsSamePage() {
        anonymous();
        var page = new PageImpl<>(List.of(item(1L, "Мяч", 1490)), PageRequest.of(0, 5), 1);
        when(itemService.findPage("", SortOption.NO, 1, 5)).thenReturn(Mono.just(page));

        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч"));
    }

    @Test
    void authenticatedUserSeesNameAndCartControls() {
        authenticated();
        var page = new PageImpl<>(List.of(item(1L, "Мяч", 1490)), PageRequest.of(0, 5), 1);
        when(itemService.findPage("", SortOption.NO, 1, 5)).thenReturn(Mono.just(page));
        when(cartService.quantitiesByItemIds(USER_ID)).thenReturn(Mono.just(Map.of(1L, 2)));

        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("buyer"))
                .value(containsString("Выйти"));
    }

    @Test
    void anonymousUserSeesLoginLink() {
        anonymous();
        var page = new PageImpl<>(List.of(item(1L, "Мяч", 1490)), PageRequest.of(0, 5), 1);
        when(itemService.findPage("", SortOption.NO, 1, 5)).thenReturn(Mono.just(page));

        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Войти"))
                .value(containsString("Войти, чтобы купить"));
    }

    @Test
    void itemsPagePassesSearchSortAndPaging() {
        anonymous();
        var page = new PageImpl<>(List.of(item(1L, "Мяч", 1490)), PageRequest.of(1, 10), 1);
        when(itemService.findPage("мяч", SortOption.ALPHA, 2, 10)).thenReturn(Mono.just(page));

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
        anonymous();
        var page = new PageImpl<>(List.of(item(1L, "Мяч", 1490)), PageRequest.of(0, 100), 1);
        when(itemService.findPage("", SortOption.NO, 1, 100)).thenReturn(Mono.just(page));

        webTestClient.get().uri("/items?pageSize=100")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void itemsPageRejectsPageSizeAboveMax() {
        anonymous();
        webTestClient.get().uri("/items?pageSize=101")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void itemsPageRejectsPageNumberBelowMin() {
        anonymous();
        webTestClient.get().uri("/items?pageNumber=0")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void itemPageRendersSingleItem() {
        anonymous();
        when(itemService.findById(1L)).thenReturn(Mono.just(item(1L, "Мяч", 1490)));

        webTestClient.get().uri("/items/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч"));
    }

    @Test
    void itemPageReturns404WhenMissing() {
        anonymous();
        when(itemService.findById(999L)).thenReturn(Mono.error(new NotFoundException("нет")));

        webTestClient.get().uri("/items/999")
                .exchange()
                .expectStatus().isNotFound();
    }
}
