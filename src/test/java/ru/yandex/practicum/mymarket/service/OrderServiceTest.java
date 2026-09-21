package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartService cartService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, cartService);
    }

    private Item item(long id, long price) {
        Item item = new Item("Товар " + id, "Описание", "images/ball.svg", price);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private CartItem cartItem(long id, Item item, int quantity) {
        CartItem cartItem = new CartItem(item, quantity);
        ReflectionTestUtils.setField(cartItem, "id", id);
        return cartItem;
    }

    @Test
    void createOrderFromCartSavesOrderAndClearsCart() {
        Item ball = item(1L, 1490);
        Item doll = item(2L, 1890);
        when(cartService.getCartItems()).thenReturn(List.of(
                cartItem(1L, ball, 2),
                cartItem(2L, doll, 1)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        OrderDto order = orderService.createOrderFromCart();

        verify(cartService).clear();
        verify(orderRepository).save(any(Order.class));
        assertThat(order.id()).isEqualTo(1L);
        assertThat(order.totalSum()).isEqualTo(2L * 1490 + 1890);
        assertThat(order.items()).hasSize(2);
    }

    @Test
    void createOrderFromEmptyCartThrows() {
        when(cartService.getCartItems()).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.createOrderFromCart())
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void getOrderReturnsDto() {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 1L);
        order.setTotalSum(1000);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderDto dto = orderService.getOrder(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.totalSum()).isEqualTo(1000);
    }

    @Test
    void getOrderThrowsWhenMissing() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOrdersReturnsNewestFirst() {
        Order first = new Order();
        ReflectionTestUtils.setField(first, "id", 1L);
        Order second = new Order();
        ReflectionTestUtils.setField(second, "id", 2L);
        when(orderRepository.findAllByOrderByIdDesc()).thenReturn(List.of(second, first));

        List<OrderDto> orders = orderService.getOrders();

        assertThat(orders).extracting(OrderDto::id).containsExactly(2L, 1L);
    }
}