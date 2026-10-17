package ru.yandex.practicum.mymarket.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

import ru.yandex.practicum.mymarket.model.UserPrincipal;
import ru.yandex.practicum.mymarket.repository.UserRepository;

import reactor.core.publisher.Mono;

@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<UserPrincipal> get() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(this::isAuthenticated)
                .flatMap(this::toPrincipal);
    }

    public Mono<Long> getUserId() {
        return get().map(UserPrincipal::getId);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private Mono<UserPrincipal> toPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Mono.just(principal);
        }
        return userRepository.findByUsername(authentication.getName()).map(UserPrincipal::of);
    }
}
