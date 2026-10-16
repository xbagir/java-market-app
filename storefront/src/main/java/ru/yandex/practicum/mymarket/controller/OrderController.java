package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ru.yandex.practicum.mymarket.service.OrderService;

import reactor.core.publisher.Mono;

@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public Mono<String> orders(Model model) {
        return orderService.getOrders()
                .doOnNext(orders -> model.addAttribute("orders", orders))
                .thenReturn("orders");
    }

    @GetMapping("/orders/{id}")
    public Mono<String> order(@PathVariable long id,
                              @RequestParam(name = "newOrder", defaultValue = "false") boolean newOrder,
                              Model model) {
        return orderService.getOrder(id)
                .doOnNext(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("newOrder", newOrder);
                })
                .thenReturn("order");
    }

    @PostMapping("/buy")
    public Mono<String> buy() {
        return orderService.createOrderFromCart()
                .map(order -> "redirect:/orders/" + order.id() + "?newOrder=true");
    }
}
