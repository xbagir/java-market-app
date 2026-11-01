package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.InsufficientFundsException;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.exception.PaymentUnavailableException;
import ru.yandex.practicum.mymarket.exception.PurchaseInProgressException;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartService cartService;

    @Mock
    private PaymentGateway paymentGateway;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        lenient().when(paymentGateway.getBalance(anyLong())).thenReturn(Mono.just(Long.MAX_VALUE));
        lenient().when(paymentGateway.charge(anyLong(), anyLong())).thenReturn(Mono.empty());
        orderService = new OrderService(orderRepository, orderItemRepository, itemRepository,
                cartService, paymentGateway);
    }

    private Item item(long id, long price) {
        Item item = new Item("Товар " + id, "Описание", "images/ball.svg", price);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private CartItem cartItem(long id, long itemId, int quantity) {
        CartItem cartItem = new CartItem(USER_ID, itemId, quantity);
        ReflectionTestUtils.setField(cartItem, "id", id);
        return cartItem;
    }

    private Order order(long id, long userId) {
        Order order = new Order(userId);
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    @Test
    void createOrderFromCartSavesOrderAndClearsCart() {
        Item ball = item(1L, 1490);
        Item doll = item(2L, 1890);
        when(cartService.getCartItems(USER_ID)).thenReturn(Flux.just(
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
        when(cartService.clear(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.createOrderFromCart(USER_ID))
                .assertNext(order -> {
                    assertThat(order.id()).isEqualTo(1L);
                    assertThat(order.totalSum()).isEqualTo(2L * 1490 + 1890);
                    assertThat(order.items()).hasSize(2);
                })
                .verifyComplete();

        verify(paymentGateway).charge(USER_ID, 2L * 1490 + 1890);
        verify(cartService).clear(USER_ID);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrderFromEmptyCartThrows() {
        when(cartService.getCartItems(USER_ID)).thenReturn(Flux.empty());

        StepVerifier.create(orderService.createOrderFromCart(USER_ID))
                .expectError(EmptyCartException.class)
                .verify();
    }

    @Test
    void createOrderFailsWhenBalanceTooLow() {
        Item ball = item(1L, 1490);
        when(cartService.getCartItems(USER_ID)).thenReturn(Flux.just(cartItem(1L, 1L, 2)));
        when(itemRepository.findAllById(org.mockito.Mockito.<Iterable<Long>>any()))
                .thenReturn(Flux.just(ball));
        stubSavedOrder(5L);
        when(paymentGateway.getBalance(USER_ID)).thenReturn(Mono.just(1000L));

        StepVerifier.create(orderService.createOrderFromCart(USER_ID))
                .expectError(InsufficientFundsException.class)
                .verify();

        verify(paymentGateway, never()).charge(anyLong(), anyLong());
        verify(orderItemRepository).deleteByOrderId(5L);
        verify(orderRepository).deleteById(5L);
        verify(cartService, never()).clear(anyLong());
    }

    @Test
    void createOrderRollsBackWhenPaymentServiceIsDown() {
        Item ball = item(1L, 1490);
        when(cartService.getCartItems(USER_ID)).thenReturn(Flux.just(cartItem(1L, 1L, 2)));
        when(itemRepository.findAllById(org.mockito.Mockito.<Iterable<Long>>any()))
                .thenReturn(Flux.just(ball));
        stubSavedOrder(6L);
        when(paymentGateway.getBalance(USER_ID))
                .thenReturn(Mono.error(new PaymentUnavailableException("down")));

        StepVerifier.create(orderService.createOrderFromCart(USER_ID))
                .expectError(PaymentUnavailableException.class)
                .verify();

        verify(orderItemRepository).deleteByOrderId(6L);
        verify(orderRepository).deleteById(6L);
        verify(cartService, never()).clear(anyLong());
    }

    @Test
    void concurrentPurchaseForSameUserIsRejected() {
        when(cartService.getCartItems(USER_ID)).thenReturn(Flux.never());

        Disposable inFlight = orderService.createOrderFromCart(USER_ID).subscribe();

        try {
            StepVerifier.create(orderService.createOrderFromCart(USER_ID))
                    .expectError(PurchaseInProgressException.class)
                    .verify();
        } finally {
            inFlight.dispose();
        }
    }

    private void stubSavedOrder(long orderId) {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", orderId);
            return Mono.just(saved);
        });
        when(orderItemRepository.saveAll(org.mockito.Mockito.<Iterable<OrderItem>>any()))
                .thenReturn(Flux.empty());
        when(orderItemRepository.deleteByOrderId(orderId)).thenReturn(Mono.just(1));
        when(orderRepository.deleteById(orderId)).thenReturn(Mono.empty());
    }

    @Test
    void getOrderReturnsDto() {
        Order order = order(1L, USER_ID);
        order.setTotalSum(1000);
        Item ball = item(1L, 1490);
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L))
                .thenReturn(Flux.just(new OrderItem(1L, 1L, 2, 1490)));
        when(itemRepository.findAllById(org.mockito.Mockito.<Iterable<Long>>any()))
                .thenReturn(Flux.just(ball));

        StepVerifier.create(orderService.getOrder(USER_ID, 1L))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.totalSum()).isEqualTo(1000);
                    assertThat(dto.items()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void getOrderHidesOrdersOfOtherUsers() {
        Order foreign = order(1L, USER_ID + 1);
        when(orderRepository.findById(1L)).thenReturn(Mono.just(foreign));

        StepVerifier.create(orderService.getOrder(USER_ID, 1L))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void getOrderThrowsWhenMissing() {
        when(orderRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrder(USER_ID, 999L))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void getOrdersReturnsNewestFirst() {
        Order first = order(1L, USER_ID);
        Order second = order(2L, USER_ID);
        when(orderRepository.findAllByUserIdOrderByIdDesc(USER_ID))
                .thenReturn(Flux.just(second, first));
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(Flux.empty());
        when(itemRepository.findAllById(org.mockito.Mockito.<Iterable<Long>>any()))
                .thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrders(USER_ID))
                .assertNext(orders -> assertThat(orders)
                        .extracting(OrderDto::id)
                        .containsExactly(2L, 1L))
                .verifyComplete();
    }

    @Test
    void getOrdersReturnsEmptyListWhenNoOrders() {
        when(orderRepository.findAllByUserIdOrderByIdDesc(USER_ID)).thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrders(USER_ID))
                .assertNext(orders -> assertThat(orders).isEmpty())
                .verifyComplete();
    }
}
