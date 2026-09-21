package ru.yandex.practicum.mymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartService cartService;

    public OrderService(OrderRepository orderRepository, CartService cartService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
    }

    public OrderDto createOrderFromCart() {
        List<CartItem> cartItems = cartService.getCartItems();
        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Корзина пуста — нечего покупать");
        }
        Order order = new Order();
        long totalSum = 0;
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem(cartItem.getItem(),
                    cartItem.getQuantity(), cartItem.getItem().getPrice());
            order.addItem(orderItem);
            totalSum += orderItem.getPrice() * orderItem.getQuantity();
        }
        order.setTotalSum(totalSum);
        Order saved = orderRepository.save(order);
        log.info("Created order {} with total {} and {} items", saved.getId(), totalSum, cartItems.size());
        cartService.clear();
        return OrderDto.of(saved);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(long id) {
        return orderRepository.findById(id)
                .map(OrderDto::of)
                .orElseThrow(() -> new NotFoundException("Заказ с id " + id + " не найден"));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrders() {
        return orderRepository.findAllByOrderByIdDesc().stream()
                .map(OrderDto::of)
                .toList();
    }
}
