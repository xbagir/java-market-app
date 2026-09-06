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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private CartItem cartItem(long id, Item item, int quantity) {
        CartItem cartItem = new CartItem(item, quantity);
        ReflectionTestUtils.setField(cartItem, "id", id);
        return cartItem;
    }

    @Test
    void plusCreatesCartItemWhenAbsent() {
        Item item = item(1L, 1490);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        cartService.update(1L, Action.PLUS);

        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void plusIncrementsExistingQuantity() {
        Item item = item(1L, 1490);
        CartItem cartItem = cartItem(1L, item, 1);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem));

        cartService.update(1L, Action.PLUS);

        assertThat(cartItem.getQuantity()).isEqualTo(2);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void minusDecrementsQuantityAboveOne() {
        Item item = item(1L, 1490);
        CartItem cartItem = cartItem(1L, item, 2);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem));

        cartService.update(1L, Action.MINUS);

        assertThat(cartItem.getQuantity()).isEqualTo(1);
        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void minusDeletesWhenQuantityIsOne() {
        Item item = item(1L, 1490);
        CartItem cartItem = cartItem(1L, item, 1);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem));

        cartService.update(1L, Action.MINUS);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void deleteRemovesCartItem() {
        Item item = item(1L, 1490);
        CartItem cartItem = cartItem(1L, item, 3);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem));

        cartService.update(1L, Action.DELETE);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void minusOnMissingCartItemIsNoOp() {
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());

        cartService.update(1L, Action.MINUS);

        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void plusOnMissingItemThrows() {
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.update(1L, Action.PLUS))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void totalSumsPriceTimesQuantity() {
        Item ball = item(1L, 1490);
        Item doll = item(2L, 1890);
        when(cartItemRepository.findAll()).thenReturn(List.of(
                cartItem(1L, ball, 2),
                cartItem(2L, doll, 1)));

        assertThat(cartService.getTotal()).isEqualTo(2L * 1490 + 1890);
    }
}