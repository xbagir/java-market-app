package ru.yandex.practicum.mymarket.security;

import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@ControllerAdvice
public class SecurityControllerAdvice {

    static final String CSRF_ATTRIBUTE_NAME = "_csrf";

    @ModelAttribute(CSRF_ATTRIBUTE_NAME)
    public Mono<CsrfToken> csrfToken(ServerWebExchange exchange) {
        return exchange.getAttributeOrDefault(CsrfToken.class.getName(), Mono.empty());
    }
}
