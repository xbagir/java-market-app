package ru.yandex.practicum.mymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.CartView;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.ItemQuantity;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public CartService(CartItemRepository cartItemRepository, ItemRepository itemRepository) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
    }

    public void update(long itemId, Action action) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Товар с id " + itemId + " не найден"));
        CartItem cartItem = cartItemRepository.findByItemId(itemId).orElse(null);
        switch (action) {
            case PLUS -> {
                if (cartItem == null) {
                    log.debug("Adding item {} to cart", itemId);
                    cartItemRepository.save(new CartItem(item, 1));
                } else {
                    log.debug("Incrementing item {} in cart", itemId);
                    cartItem.setQuantity(cartItem.getQuantity() + 1);
                }
            }
            case MINUS -> {
                if (cartItem != null) {
                    if (cartItem.getQuantity() > 1) {
                        log.debug("Decrementing item {} in cart", itemId);
                        cartItem.setQuantity(cartItem.getQuantity() - 1);
                    } else {
                        log.debug("Removing item {} from cart (quantity was 1)", itemId);
                        cartItemRepository.delete(cartItem);
                    }
                }
            }
            case DELETE -> {
                if (cartItem != null) {
                    log.debug("Deleting item {} from cart", itemId);
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
    public CartView getCartView() {
        List<CartItem> rows = cartItemRepository.findAll();
        List<ItemDto> items = rows.stream()
                .map(ci -> ItemDto.of(ci.getItem(), ci.getQuantity()))
                .toList();
        long total = rows.stream()
                .mapToLong(ci -> ci.getItem().getPrice() * ci.getQuantity())
                .sum();
        return new CartView(items, total);
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> quantitiesByItemIds(List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return cartItemRepository.findQuantitiesByItemIds(itemIds).stream()
                .collect(Collectors.toMap(ItemQuantity::itemId, ItemQuantity::quantity, Integer::sum));
    }

    @Transactional(readOnly = true)
    public int getQuantityByItemId(long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .map(CartItem::getQuantity)
                .orElse(0);
    }

    public void clear() {
        log.info("Clearing cart");
        cartItemRepository.deleteAll();
    }
}
