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
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.UserPrincipal;
import ru.yandex.practicum.mymarket.security.CurrentUserProvider;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.UserService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

import reactor.core.publisher.Mono;

@WebFluxTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    private static final long USER_ID = 7L;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

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
    }

    @Test
    void ordersPageRendersList() {
        when(orderService.getOrders(USER_ID))
                .thenReturn(Mono.just(List.of(new OrderDto(1L, List.of(), 1000))));

        webTestClient.mutateWith(mockUser("buyer").roles("USER"))
                .get().uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Заказ"));
    }

    @Test
    void orderPageRendersWithNewOrderFlag() {
        when(orderService.getOrder(USER_ID, 1L))
                .thenReturn(Mono.just(new OrderDto(1L, List.of(), 1000)));

        webTestClient.mutateWith(mockUser("buyer").roles("USER"))
                .get().uri("/orders/1?newOrder=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Поздравляем"));
    }

    @Test
    void orderPageDefaultsNewOrderToFalse() {
        when(orderService.getOrder(USER_ID, 1L))
                .thenReturn(Mono.just(new OrderDto(1L, List.of(), 1000)));

        webTestClient.mutateWith(mockUser("buyer").roles("USER"))
                .get().uri("/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("1000"));
    }

    @Test
    void orderPageReturns404WhenMissing() {
        when(orderService.getOrder(USER_ID, 999L))
                .thenReturn(Mono.error(new NotFoundException("нет")));

        webTestClient.mutateWith(mockUser("buyer").roles("USER"))
                .get().uri("/orders/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void anonymousUserIsRedirectedToLogin() {
        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/login");
    }

    @Test
    void buyRedirectsToCreatedOrder() {
        when(orderService.createOrderFromCart(USER_ID))
                .thenReturn(Mono.just(new OrderDto(42L, List.of(), 5000)));

        webTestClient.mutateWith(mockUser("buyer").roles("USER")).mutateWith(csrf())
                .post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/orders/42?newOrder=true");
    }

    @Test
    void buyEmptyCartReturns400() {
        when(orderService.createOrderFromCart(USER_ID))
                .thenReturn(Mono.error(new EmptyCartException("пусто")));

        webTestClient.mutateWith(mockUser("buyer").roles("USER")).mutateWith(csrf())
                .post().uri("/buy")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
