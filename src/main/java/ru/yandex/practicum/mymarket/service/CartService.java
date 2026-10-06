package ru.yandex.practicum.mymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.CartView;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public CartService(CartItemRepository cartItemRepository, ItemRepository itemRepository) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public Mono<Void> update(long itemId, Action action) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new NotFoundException("Товар с id " + itemId + " не найден")))
                .flatMap(item -> cartItemRepository.findByItemId(itemId)
                        .flatMap(cartItem -> applyAction(cartItem, action).thenReturn(Boolean.TRUE))
                        .defaultIfEmpty(Boolean.FALSE)
                        .flatMap(found -> {
                            if (found || action != Action.PLUS) {
                                return Mono.empty();
                            }
                            log.debug("Adding item {} to cart", itemId);
                            return cartItemRepository.save(new CartItem(itemId, 1)).then();
                        }));
    }

    private Mono<Void> applyAction(CartItem cartItem, Action action) {
        long itemId = cartItem.getItemId();
        return switch (action) {
            case PLUS -> {
                log.debug("Incrementing item {} in cart", itemId);
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                yield cartItemRepository.save(cartItem).then();
            }
            case MINUS -> {
                if (cartItem.getQuantity() > 1) {
                    log.debug("Decrementing item {} in cart", itemId);
                    cartItem.setQuantity(cartItem.getQuantity() - 1);
                    yield cartItemRepository.save(cartItem).then();
                }
                log.debug("Removing item {} from cart (quantity was 1)", itemId);
                yield cartItemRepository.delete(cartItem);
            }
            case DELETE -> {
                log.debug("Deleting item {} from cart", itemId);
                yield cartItemRepository.delete(cartItem);
            }
        };
    }

    public Flux<CartItem> getCartItems() {
        return cartItemRepository.findAll();
    }

    public Mono<CartView> getCartView() {
        return cartItemRepository.findAll()
                .collectMap(CartItem::getItemId, CartItem::getQuantity)
                .flatMap(quantities -> {
                    if (quantities.isEmpty()) {
                        return Mono.just(new CartView(List.of(), 0));
                    }
                    return itemRepository.findAllByIdOrdered(quantities.keySet())
                            .collectList()
                            .map(items -> {
                                List<ItemDto> dtos = items.stream()
                                        .map(item -> ItemDto.of(item,
                                                quantities.getOrDefault(item.getId(), 0)))
                                        .toList();
                                long total = items.stream()
                                        .mapToLong(item -> item.getPrice()
                                                * quantities.getOrDefault(item.getId(), 0))
                                        .sum();
                                return new CartView(dtos, total);
                            });
                });
    }

    public Mono<Map<Long, Integer>> quantitiesByItemIds() {
        return cartItemRepository.findAll()
                .collectMap(CartItem::getItemId, CartItem::getQuantity);
    }

    public Mono<Integer> getQuantityByItemId(long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .map(CartItem::getQuantity)
                .defaultIfEmpty(0);
    }

    public Mono<Void> clear() {
        log.info("Clearing cart");
        return cartItemRepository.deleteAll();
    }
}
