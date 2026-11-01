package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.practicum.mymarket.config.SecurityConfig;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.CartView;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.exception.PaymentUnavailableException;
import ru.yandex.practicum.mymarket.model.UserPrincipal;
import ru.yandex.practicum.mymarket.security.CurrentUserProvider;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.PaymentGateway;
import ru.yandex.practicum.mymarket.service.UserService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

import reactor.core.publisher.Mono;

@WebFluxTest(CartController.class)
@Import(SecurityConfig.class)
class CartControllerTest {

    private static final long USER_ID = 7L;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(USER_ID, "buyer", "secret", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        lenient().when(currentUserProvider.getUserId()).thenReturn(Mono.just(USER_ID));
        lenient().when(currentUserProvider.get()).thenReturn(Mono.just(principal));
        lenient().when(paymentGateway.getBalance(USER_ID)).thenReturn(Mono.just(100_000L));
    }

    private ItemDto item(long id, String title, long price, int count) {
        return new ItemDto(id, title, "Описание " + title, "images/ball.svg", price, count);
    }

    @Test
    void cartPageRendersItemsAndTotal() {
        when(cartService.getCartView(USER_ID)).thenReturn(Mono.just(new CartView(
                List.of(item(1L, "Мяч", 1490, 2)), 2L * 1490)));

        webTestClient.mutateWith(mockUser("buyer").roles("USER"))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Мяч"))
                .value(not(containsString("disabled=\"disabled\"")))
                .value(not(containsString("Недостаточно средств")));
    }

    @Test
    void cartAliasPathWorksToo() {
        when(cartService.getCartView(USER_ID)).thenReturn(Mono.just(new CartView(List.of(), 0L)));

        webTestClient.mutateWith(mockUser("buyer").roles("USER"))
                .get().uri("/cart")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Баланс: 100000 руб."));
    }

    @Test
    void anonymousUserIsRedirectedToLogin() {
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/login");
    }

    @Test
    void cartPageDisablesOrderWhenBalanceTooLow() {
        when(cartService.getCartView(USER_ID)).thenReturn(Mono.just(new CartView(
                List.of(item(1L, "Мяч", 1490, 2)), 2L * 1490)));
        when(paymentGateway.getBalance(USER_ID)).thenReturn(Mono.just(10L));

        webTestClient.mutateWith(mockUser("buyer").roles("USER"))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Недостаточно средств на балансе"))
                .value(containsString("disabled=\"disabled\""));
    }

    @Test
    void cartPageDisablesOrderWhenPaymentServiceUnavailable() {
        when(cartService.getCartView(USER_ID)).thenReturn(Mono.just(new CartView(
                List.of(item(1L, "Мяч", 1490, 2)), 2L * 1490)));
        when(paymentGateway.getBalance(USER_ID))
                .thenReturn(Mono.error(new PaymentUnavailableException("down")));

        webTestClient.mutateWith(mockUser("buyer").roles("USER"))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Сервис платежей недоступен"))
                .value(containsString("disabled=\"disabled\""));
    }

    @Test
    void postCartItemUpdatesQuantityAndRedirects() {
        when(cartService.update(USER_ID, 1L, Action.DELETE)).thenReturn(Mono.empty());

        webTestClient.mutateWith(mockUser("buyer").roles("USER")).mutateWith(csrf())
                .post().uri("/cart/items?id=1&action=DELETE")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/cart/items");

        verify(cartService).update(USER_ID, 1L, Action.DELETE);
    }

    @Test
    void postCatalogItemUpdatesCartAndRedirectsBack() {
        when(cartService.update(USER_ID, 1L, Action.PLUS)).thenReturn(Mono.empty());

        webTestClient.mutateWith(mockUser("buyer").roles("USER")).mutateWith(csrf())
                .post().uri("/items?id=1&action=PLUS&search=&sort=NO&pageNumber=1&pageSize=5")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items?search=&sort=NO&pageNumber=1&pageSize=5");

        verify(cartService).update(USER_ID, 1L, Action.PLUS);
    }

    @Test
    void postCatalogItemRejectsInvalidPageSize() {
        webTestClient.mutateWith(mockUser("buyer").roles("USER")).mutateWith(csrf())
                .post().uri("/items?id=1&action=PLUS&search=&sort=NO&pageNumber=1&pageSize=1")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void postItemPageActionRedirectsToItem() {
        when(cartService.update(USER_ID, 1L, Action.MINUS)).thenReturn(Mono.empty());

        webTestClient.mutateWith(mockUser("buyer").roles("USER")).mutateWith(csrf())
                .post().uri("/items/1?action=MINUS")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items/1");

        verify(cartService).update(USER_ID, 1L, Action.MINUS);
    }
}
