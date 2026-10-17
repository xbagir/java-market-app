package ru.yandex.practicum.mymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.InsufficientFundsException;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final CartService cartService;
    private final PaymentGateway paymentGateway;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ItemRepository itemRepository,
                        CartService cartService,
                        PaymentGateway paymentGateway) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.itemRepository = itemRepository;
        this.cartService = cartService;
        this.paymentGateway = paymentGateway;
    }

    @Transactional
    public Mono<OrderDto> createOrderFromCart(long userId) {
        return cartService.getCartItems(userId)
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new EmptyCartException("Корзина пуста — нечего покупать"));
                    }
                    List<Long> itemIds = cartItems.stream().map(CartItem::getItemId).toList();
                    return itemRepository.findAllById(itemIds)
                            .collectMap(Item::getId)
                            .flatMap(itemsById -> {
                                long totalSum = cartItems.stream()
                                        .mapToLong(ci -> itemsById.get(ci.getItemId()).getPrice()
                                                * ci.getQuantity())
                                        .sum();
                                return payForOrder(userId, totalSum)
                                        .then(Mono.defer(() -> saveOrder(userId, cartItems,
                                                itemsById, totalSum)));
                            });
                })
                .doOnSuccess(order -> log.info("Created order {} of user {} with total {}",
                        order.id(), userId, order.totalSum()));
    }

    private Mono<Void> payForOrder(long userId, long totalSum) {
        return paymentGateway.getBalance(userId)
                .flatMap(balance -> {
                    if (balance < totalSum) {
                        return Mono.<Void>error(
                                new InsufficientFundsException(totalSum, balance));
                    }
                    return paymentGateway.charge(userId, totalSum);
                });
    }

    private Mono<OrderDto> saveOrder(long userId, List<CartItem> cartItems,
                                     Map<Long, Item> itemsById,
                                     long totalSum) {
        Order order = new Order(userId);
        order.setTotalSum(totalSum);
        return orderRepository.save(order)
                .flatMap(saved -> {
                    List<OrderItem> orderItems = cartItems.stream()
                            .map(ci -> new OrderItem(saved.getId(), ci.getItemId(),
                                    ci.getQuantity(),
                                    itemsById.get(ci.getItemId()).getPrice()))
                            .toList();
                    return orderItemRepository.saveAll(orderItems)
                            .then(cartService.clear(userId))
                            .thenReturn(OrderDto.of(saved, orderItems, itemsById));
                });
    }

    public Mono<OrderDto> getOrder(long userId, long id) {
        return orderRepository.findById(id)
                .filter(order -> order.getUserId() == userId)
                .switchIfEmpty(Mono.error(new NotFoundException("Заказ с id " + id + " не найден")))
                .flatMap(this::toOrderDto);
    }

    public Mono<List<OrderDto>> getOrders(long userId) {
        return orderRepository.findAllByUserIdOrderByIdDesc(userId).collectList()
                .flatMap(orders -> {
                    if (orders.isEmpty()) {
                        return Mono.just(List.<OrderDto>of());
                    }
                    List<Long> orderIds = orders.stream().map(Order::getId).toList();
                    return orderItemRepository.findByOrderIdIn(orderIds).collectList()
                            .flatMap(orderItems -> {
                                List<Long> itemIds = orderItems.stream()
                                        .map(OrderItem::getItemId)
                                        .distinct()
                                        .toList();
                                return itemRepository.findAllById(itemIds)
                                        .collectMap(Item::getId)
                                        .map(itemsById -> orders.stream()
                                                .map(order -> OrderDto.of(order, orderItems.stream()
                                                        .filter(oi -> oi.getOrderId() == order.getId())
                                                        .toList(), itemsById))
                                                .toList());
                            });
                });
    }

    private Mono<OrderDto> toOrderDto(Order order) {
        return orderItemRepository.findByOrderId(order.getId())
                .collectList()
                .flatMap(orderItems -> {
                    List<Long> itemIds = orderItems.stream()
                            .map(OrderItem::getItemId)
                            .distinct()
                            .toList();
                    return itemRepository.findAllById(itemIds)
                            .collectMap(Item::getId)
                            .map(itemsById -> OrderDto.of(order, orderItems, itemsById));
                });
    }
}
