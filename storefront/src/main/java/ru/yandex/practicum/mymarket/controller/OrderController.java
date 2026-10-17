package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ru.yandex.practicum.mymarket.security.CurrentUserProvider;
import ru.yandex.practicum.mymarket.service.OrderService;

import reactor.core.publisher.Mono;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserProvider currentUserProvider;

    public OrderController(OrderService orderService, CurrentUserProvider currentUserProvider) {
        this.orderService = orderService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/orders")
    public Mono<String> orders(Model model) {
        return currentUserProvider.getUserId()
                .flatMap(userId -> orderService.getOrders(userId)
                        .doOnNext(orders -> model.addAttribute("orders", orders))
                        .then(currentUserProvider.get().map(principal -> {
                            model.addAttribute("authenticated", true);
                            model.addAttribute("username", principal.getUsername());
                            return "orders";
                        })));
    }

    @GetMapping("/orders/{id}")
    public Mono<String> order(@PathVariable long id,
                              @RequestParam(name = "newOrder", defaultValue = "false") boolean newOrder,
                              Model model) {
        return currentUserProvider.getUserId()
                .flatMap(userId -> orderService.getOrder(userId, id)
                        .doOnNext(order -> {
                            model.addAttribute("order", order);
                            model.addAttribute("newOrder", newOrder);
                        })
                        .then(currentUserProvider.get().map(principal -> {
                            model.addAttribute("authenticated", true);
                            model.addAttribute("username", principal.getUsername());
                            return "order";
                        })));
    }

    @PostMapping("/buy")
    public Mono<String> buy() {
        return currentUserProvider.getUserId()
                .flatMap(userId -> orderService.createOrderFromCart(userId))
                .map(order -> "redirect:/orders/" + order.id() + "?newOrder=true");
    }
}
