package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.yandex.practicum.mymarket.model.Item;

public interface ItemRepository extends ReadOnlyRepository<Item, Long>, JpaRepository<Item, Long> {

    Page<Item> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title, String description, Pageable pageable);
}