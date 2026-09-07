package ru.yandex.practicum.mymarket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ReadOnlyRepository<T, ID> {

    Optional<T> findById(ID id);

    List<T> findAll();

    List<T> findAllById(Iterable<ID> ids);

    long count();

    boolean existsById(ID id);
}
