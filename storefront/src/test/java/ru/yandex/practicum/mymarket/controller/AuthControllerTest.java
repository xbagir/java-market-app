package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import ru.yandex.practicum.mymarket.config.SecurityConfig;
import ru.yandex.practicum.mymarket.exception.UsernameAlreadyUsedException;
import ru.yandex.practicum.mymarket.model.UserPrincipal;
import ru.yandex.practicum.mymarket.service.UserService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import reactor.core.publisher.Mono;

@WebFluxTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserService userService;

    @Test
    void loginPageIsPublic() {
        webTestClient.get().uri("/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Войти"))
                .value(containsString("Зарегистрироваться"));
    }

    @Test
    void registerPageIsPublic() {
        webTestClient.get().uri("/register")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("Зарегистрироваться"));
    }

    @Test
    void registerCreatesUserAndRedirectsToLogin() {
        when(userService.register("newbie", "pass"))
                .thenReturn(Mono.just(new UserPrincipal(1L, "newbie", "pass", true, List.of())));

        webTestClient.mutateWith(csrf())
                .post().uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "newbie")
                        .with("password", "pass"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/login?registered");
    }

    @Test
    void registerWithoutCsrfTokenIsForbidden() {
        webTestClient.post().uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "newbie")
                        .with("password", "pass"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void registerShowsErrorWhenUsernameTaken() {
        when(userService.register("taken", "pass"))
                .thenReturn(Mono.error(new UsernameAlreadyUsedException(
                        "Пользователь taken уже существует")));

        webTestClient.mutateWith(csrf())
                .post().uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "taken")
                        .with("password", "pass"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("уже существует"));
    }
}
