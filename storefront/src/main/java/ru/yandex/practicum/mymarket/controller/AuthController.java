package ru.yandex.practicum.mymarket.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import ru.yandex.practicum.mymarket.dto.RegisterForm;
import ru.yandex.practicum.mymarket.exception.UsernameAlreadyUsedException;
import ru.yandex.practicum.mymarket.service.UserService;

import reactor.core.publisher.Mono;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public Mono<String> login(Model model) {
        model.addAttribute("authenticated", false);
        return Mono.just("login");
    }

    @GetMapping("/register")
    public Mono<String> registerForm(Model model) {
        model.addAttribute("authenticated", false);
        model.addAttribute("form", new RegisterForm());
        return Mono.just("register");
    }

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> register(@ModelAttribute("form") RegisterForm form, Model model) {
        model.addAttribute("authenticated", false);
        return userService.register(form.getUsername(), form.getPassword())
                .thenReturn("redirect:/login?registered")
                .onErrorResume(UsernameAlreadyUsedException.class, ex -> {
                    model.addAttribute("error", ex.getMessage());
                    return Mono.just("register");
                });
    }
}
