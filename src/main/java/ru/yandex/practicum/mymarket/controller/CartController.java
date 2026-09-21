package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.CartView;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping({"/cart", "/cart/items"})
    public String cart(Model model) {
        CartView cart = cartService.getCartView();
        model.addAttribute("items", cart.items());
        model.addAttribute("total", cart.total());
        return "cart";
    }

    @PostMapping("/cart/items")
    public String updateCartItem(@RequestParam long id, @RequestParam Action action) {
        cartService.update(id, action);
        return "redirect:/cart/items";
    }

    @PostMapping("/items")
    public String updateItemFromCatalog(@RequestParam long id,
                                        @RequestParam Action action,
                                        @RequestParam(name = "search", defaultValue = "") String search,
                                        @RequestParam(name = "sort", defaultValue = "NO") SortOption sort,
                                        @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
                                        @RequestParam(name = "pageSize", defaultValue = "5") int pageSize,
                                        RedirectAttributes redirectAttributes) {
        cartService.update(id, action);
        redirectAttributes.addAttribute("search", search);
        redirectAttributes.addAttribute("sort", sort.name());
        redirectAttributes.addAttribute("pageNumber", pageNumber);
        redirectAttributes.addAttribute("pageSize", pageSize);
        return "redirect:/items";
    }

    @PostMapping("/items/{id}")
    public String updateItemFromPage(@PathVariable long id, @RequestParam Action action) {
        cartService.update(id, action);
        return "redirect:/items/" + id;
    }
}