package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartService cartService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, itemRepository, cartService);
    }

    private Item item(long id, long price) {
        Item item = new Item("Товар " + id, "Описание", "images/ball.svg", price);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private CartItem cartItem(long id, long itemId, int quantity) {
        CartItem cartItem = new CartItem(itemId, quantity);
        ReflectionTestUtils.setField(cartItem, "id", id);
        return cartItem;
    }

    @Test
    void createOrderFromCartSavesOrderAndClearsCart() {
        Item ball = item(1L, 1490);
        Item doll = item(2L, 1890);
        when(cartService.getCartItems()).thenReturn(Flux.just(
                cartItem(1L, 1L, 2),
                cartItem(2L, 2L, 1)));
        when(itemRepository.findAllById(org.mockito.Mockito.<Iterable<Long>>any()))
                .thenReturn(Flux.just(ball, doll));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return Mono.just(saved);
        });
        when(orderItemRepository.saveAll(org.mockito.Mockito.<Iterable<OrderItem>>any()))
                .thenAnswer(invocation ->
                        Flux.fromIterable(invocation.getArgument(0)));
        when(cartService.clear()).thenReturn(Mono.empty());

        StepVerifier.create(orderService.createOrderFromCart())
                .assertNext(order -> {
                    assertThat(order.id()).isEqualTo(1L);
                    assertThat(order.totalSum()).isEqualTo(2L * 1490 + 1890);
                    assertThat(order.items()).hasSize(2);
                })
                .verifyComplete();

        verify(cartService).clear();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrderFromEmptyCartThrows() {
        when(cartService.getCartItems()).thenReturn(Flux.empty());

        StepVerifier.create(orderService.createOrderFromCart())
                .expectError(EmptyCartException.class)
                .verify();
    }

    @Test
    void getOrderReturnsDto() {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 1L);
        order.setTotalSum(1000);
        Item ball = item(1L, 1490);
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L))
                .thenReturn(Flux.just(new OrderItem(1L, 1L, 2, 1490)));
        when(itemRepository.findAllById(org.mockito.Mockito.<Iterable<Long>>any()))
                .thenReturn(Flux.just(ball));

        StepVerifier.create(orderService.getOrder(1L))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.totalSum()).isEqualTo(1000);
                    assertThat(dto.items()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void getOrderThrowsWhenMissing() {
        when(orderRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrder(999L))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void getOrdersReturnsNewestFirst() {
        Order first = new Order();
        ReflectionTestUtils.setField(first, "id", 1L);
        Order second = new Order();
        ReflectionTestUtils.setField(second, "id", 2L);
        when(orderRepository.findAllByOrderByIdDesc()).thenReturn(Flux.just(second, first));
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(Flux.empty());
        when(itemRepository.findAllById(org.mockito.Mockito.<Iterable<Long>>any()))
                .thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrders())
                .assertNext(orders -> assertThat(orders)
                        .extracting(ru.yandex.practicum.mymarket.dto.OrderDto::id)
                        .containsExactly(2L, 1L))
                .verifyComplete();
    }

    @Test
    void getOrdersReturnsEmptyListWhenNoOrders() {
        when(orderRepository.findAllByOrderByIdDesc()).thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrders())
                .assertNext(orders -> assertThat(orders).isEmpty())
                .verifyComplete();
    }
}
