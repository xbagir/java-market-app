package ru.yandex.practicum.mymarket.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ru.yandex.practicum.mymarket.exception.UsernameAlreadyUsedException;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.model.UserPrincipal;
import ru.yandex.practicum.mymarket.repository.UserRepository;

import reactor.core.publisher.Mono;

@Service
public class UserService implements ReactiveUserDetailsService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserPrincipal::of)
                .cast(UserDetails.class);
    }

    public Mono<UserPrincipal> register(String username, String rawPassword) {
        String normalized = username == null ? "" : username.trim();
        return userRepository.findByUsername(normalized)
                .flatMap(existing -> Mono.<UserPrincipal>error(
                        new UsernameAlreadyUsedException("Пользователь " + normalized + " уже существует")))
                .switchIfEmpty(Mono.defer(() -> {
                    User user = new User(normalized, passwordEncoder.encode(rawPassword), true,
                            DEFAULT_ROLE);
                    return userRepository.save(user)
                            .map(UserPrincipal::of)
                            .onErrorMap(DataIntegrityViolationException.class, ex -> duplicate(normalized));
                }));
    }

    private UsernameAlreadyUsedException duplicate(String username) {
        return new UsernameAlreadyUsedException("Пользователь " + username + " уже существует");
    }
}
