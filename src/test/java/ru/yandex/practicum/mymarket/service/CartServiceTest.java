package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemRepository, itemRepository);
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
    void plusCreatesCartItemWhenAbsent() {
        Item item = item(1L, 1490);
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.empty());
        when(cartItemRepository.save(any(CartItem.class)))
                .thenReturn(Mono.just(cartItem(1L, 1L, 1)));

        StepVerifier.create(cartService.update(1L, Action.PLUS))
                .verifyComplete();

        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void plusIncrementsExistingQuantity() {
        Item item = item(1L, 1490);
        CartItem cartItem = cartItem(1L, 1L, 1);
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.save(cartItem)).thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.update(1L, Action.PLUS))
                .verifyComplete();

        assertThat(cartItem.getQuantity()).isEqualTo(2);
    }

    @Test
    void minusDecrementsQuantityAboveOne() {
        Item item = item(1L, 1490);
        CartItem cartItem = cartItem(1L, 1L, 2);
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.save(cartItem)).thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.update(1L, Action.MINUS))
                .verifyComplete();

        assertThat(cartItem.getQuantity()).isEqualTo(1);
        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void minusDeletesWhenQuantityIsOne() {
        Item item = item(1L, 1490);
        CartItem cartItem = cartItem(1L, 1L, 1);
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.update(1L, Action.MINUS))
                .verifyComplete();

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void deleteRemovesCartItem() {
        Item item = item(1L, 1490);
        CartItem cartItem = cartItem(1L, 1L, 3);
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.update(1L, Action.DELETE))
                .verifyComplete();

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void minusOnMissingCartItemIsNoOp() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item(1L, 1490)));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.update(1L, Action.MINUS))
                .verifyComplete();

        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void updateThrowsWhenItemMissing() {
        when(itemRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.update(999L, Action.PLUS))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void getQuantityByItemIdDefaultsToZero() {
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem(1L, 1L, 3)));
        when(cartItemRepository.findByItemId(999L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.getQuantityByItemId(1L))
                .expectNext(3)
                .verifyComplete();
        StepVerifier.create(cartService.getQuantityByItemId(999L))
                .expectNext(0)
                .verifyComplete();
    }

    @Test
    void quantitiesByItemIdsCollectsMap() {
        when(cartItemRepository.findAll()).thenReturn(Flux.just(
                cartItem(1L, 1L, 2),
                cartItem(2L, 2L, 1)));

        StepVerifier.create(cartService.quantitiesByItemIds())
                .assertNext(quantities -> assertThat(quantities)
                        .containsOnlyKeys(1L, 2L)
                        .containsEntry(1L, 2)
                        .containsEntry(2L, 1))
                .verifyComplete();
    }

    @Test
    void getCartViewReturnsItemsAndTotal() {
        Item ball = item(1L, 1490);
        Item doll = item(2L, 1890);
        when(cartItemRepository.findAll()).thenReturn(Flux.just(
                cartItem(1L, 1L, 2),
                cartItem(2L, 2L, 1)));
        when(itemRepository.findAllByIdOrdered(org.mockito.Mockito.<Collection<Long>>any()))
                .thenReturn(Flux.just(ball, doll));

        StepVerifier.create(cartService.getCartView())
                .assertNext(view -> {
                    assertThat(view.items()).hasSize(2);
                    assertThat(view.items().get(0).count()).isEqualTo(2);
                    assertThat(view.total()).isEqualTo(2L * 1490 + 1890);
                })
                .verifyComplete();
    }
}
