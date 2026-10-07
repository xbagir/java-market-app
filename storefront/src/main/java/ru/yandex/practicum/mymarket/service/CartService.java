package ru.yandex.practicum.mymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.practicum.mymarket.cache.ItemCache;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.CartView;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final ItemCache itemCache;

    public CartService(CartItemRepository cartItemRepository, ItemRepository itemRepository,
                       ItemCache itemCache) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
        this.itemCache = itemCache;
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
                    return loadItems(quantities)
                            .map(items -> {
                                List<ItemDto> dtos = items.stream()
                                        .map(item -> item.withCount(
                                                quantities.getOrDefault(item.id(), 0)))
                                        .toList();
                                long total = dtos.stream()
                                        .mapToLong(item -> item.price() * item.count())
                                        .sum();
                                return new CartView(dtos, total);
                            });
                });
    }

    private Mono<List<ItemDto>> loadItems(Map<Long, Integer> quantities) {
        return Flux.fromIterable(quantities.keySet())
                .flatMap(id -> itemCache.get(ItemCache.itemKey(id), ItemDto.class)
                        .map(dto -> Map.entry(id, Optional.of(dto)))
                        .defaultIfEmpty(Map.entry(id, Optional.empty())))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(cached -> {
                    Map<Long, ItemDto> items = new HashMap<>();
                    List<Long> missing = new ArrayList<>();
                    cached.forEach((id, value) -> value
                            .ifPresentOrElse(item -> items.put(id, item), () -> missing.add(id)));
                    return loadMissing(missing)
                            .map(loaded -> {
                                items.putAll(loaded);
                                return items.entrySet().stream()
                                        .sorted(Map.Entry.comparingByKey())
                                        .map(Map.Entry::getValue)
                                        .toList();
                            });
                });
    }

    private Mono<Map<Long, ItemDto>> loadMissing(List<Long> missing) {
        if (missing.isEmpty()) {
            return Mono.just(Map.of());
        }
        log.debug("Loading {} cart items missing from cache", missing.size());
        return itemRepository.findAllByIdOrdered(missing)
                .flatMap(item -> {
                    ItemDto dto = ItemDto.of(item, 0);
                    return itemCache.put(ItemCache.itemKey(item.getId()), dto)
                            .thenReturn(dto);
                })
                .collectMap(ItemDto::id, dto -> dto);
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
