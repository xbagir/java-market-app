package ru.yandex.practicum.mymarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public CartService(CartItemRepository cartItemRepository, ItemRepository itemRepository) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
    }

    public void update(long itemId, Action action) {
        CartItem cartItem = cartItemRepository.findByItemId(itemId).orElse(null);
        switch (action) {
            case PLUS -> {
                if (cartItem == null) {
                    Item item = itemRepository.findById(itemId)
                            .orElseThrow(() -> new NotFoundException("Товар с id " + itemId + " не найден"));
                    cartItemRepository.save(new CartItem(item, 1));
                } else {
                    cartItem.setQuantity(cartItem.getQuantity() + 1);
                }
            }
            case MINUS -> {
                if (cartItem != null) {
                    if (cartItem.getQuantity() > 1) {
                        cartItem.setQuantity(cartItem.getQuantity() - 1);
                    } else {
                        cartItemRepository.delete(cartItem);
                    }
                }
            }
            case DELETE -> {
                if (cartItem != null) {
                    cartItemRepository.delete(cartItem);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems() {
        return cartItemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> quantitiesByItemId() {
        return cartItemRepository.findAll().stream()
                .collect(Collectors.toMap(ci -> ci.getItem().getId(), CartItem::getQuantity, Integer::sum));
    }

    @Transactional(readOnly = true)
    public long getTotal() {
        return cartItemRepository.findAll().stream()
                .mapToLong(ci -> ci.getItem().getPrice() * ci.getQuantity())
                .sum();
    }

    public void clear() {
        cartItemRepository.deleteAll();
    }
}