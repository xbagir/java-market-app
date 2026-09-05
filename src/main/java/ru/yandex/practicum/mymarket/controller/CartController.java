package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping({"/cart", "/cart/items"})
    public String cart(Model model) {
        List<ItemDto> items = cartService.getCartItems().stream()
                .map(ci -> new ItemDto(ci.getItem().getId(), ci.getItem().getTitle(),
                        ci.getItem().getDescription(), ci.getItem().getImgPath(),
                        ci.getItem().getPrice(), ci.getQuantity()))
                .toList();
        model.addAttribute("items", items);
        model.addAttribute("total", cartService.getTotal());
        return "cart";
    }

    @PostMapping("/cart/items")
    public String updateCartItem(@RequestParam long id, @RequestParam Action action, Model model) {
        cartService.update(id, action);
        return cart(model);
    }
}