package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.service.CartService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import reactor.core.publisher.Mono;

@Controller
@Validated
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping({"/cart", "/cart/items"})
    public Mono<String> cart(Model model) {
        return cartService.getCartView()
                .doOnNext(cart -> {
                    model.addAttribute("items", cart.items());
                    model.addAttribute("total", cart.total());
                })
                .thenReturn("cart");
    }

    @PostMapping("/cart/items")
    public Mono<String> updateCartItem(@RequestParam long id, @RequestParam Action action) {
        return cartService.update(id, action).thenReturn("redirect:/cart/items");
    }

    @PostMapping("/items")
    public Mono<String> updateItemFromCatalog(@RequestParam long id,
                                              @RequestParam Action action,
                                              @RequestParam(name = "search", defaultValue = "") String search,
                                              @RequestParam(name = "sort", defaultValue = "NO") SortOption sort,
                                              @RequestParam(name = "pageNumber", defaultValue = "1") @Min(1) int pageNumber,
                                              @RequestParam(name = "pageSize", defaultValue = "5") @Min(2) @Max(100) int pageSize) {
        return cartService.update(id, action)
                .thenReturn("redirect:" + UriComponentsBuilder.fromPath("/items")
                        .queryParam("search", search)
                        .queryParam("sort", sort.name())
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .encode().build().toUriString());
    }

    @PostMapping("/items/{id}")
    public Mono<String> updateItemFromPage(@PathVariable long id, @RequestParam Action action) {
        return cartService.update(id, action).thenReturn("redirect:/items/" + id);
    }
}
