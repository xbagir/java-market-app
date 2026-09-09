package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import ru.yandex.practicum.mymarket.dto.ItemQuantity;
import ru.yandex.practicum.mymarket.model.CartItem;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ci FROM CartItem ci WHERE ci.item.id = :itemId")
    Optional<CartItem> findByItemIdForUpdate(long itemId);

    Optional<CartItem> findByItemId(long itemId);

    @Query("SELECT new ru.yandex.practicum.mymarket.dto.ItemQuantity(ci.item.id, ci.quantity) "
            + "FROM CartItem ci WHERE ci.item.id IN :itemIds")
    List<ItemQuantity> findQuantitiesByItemIds(List<Long> itemIds);
}
