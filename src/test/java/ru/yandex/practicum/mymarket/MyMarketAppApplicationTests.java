package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class MyMarketAppApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Item ball;
    private Item doll;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        orderRepository.deleteAll();
        itemRepository.deleteAll();
        ball = itemRepository.save(new Item("Мяч футбольный", "Круглый мяч для игры", "images/ball.svg", 1490));
        doll = itemRepository.save(new Item("Кукла «Алиса»", "Нарядная кукла", "images/doll.svg", 1890));
    }

    @Test
    void contextLoads() {
    }

    @Test
    void itemsPageShowsCatalog() throws Exception {
        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(content().string(containsString("Мяч футбольный")))
                .andExpect(content().string(containsString("Кукла «Алиса»")));
    }

    @Test
    void searchFiltersCatalog() throws Exception {
        mockMvc.perform(get("/items").param("search", "мяч"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Мяч футбольный")))
                .andExpect(content().string(not(containsString("Кукла «Алиса»"))));
    }

    @Test
    void fullPurchaseFlow() throws Exception {
        addToCart(ball);
        addToCart(doll);
        addToCart(ball);

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Мяч футбольный")))
                .andExpect(content().string(containsString("Кукла «Алиса»")))
                .andExpect(content().string(containsString("4870")));

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*"));

        long orderId = orderRepository.findAllByOrderByIdDesc().get(0).getId();

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Заказ №")));

        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Заказ №" + orderId)));

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Мяч футбольный"))));
    }

    @Test
    void cartQuantityActionsWork() throws Exception {
        addToCart(ball);
        addToCart(ball);

        mockMvc.perform(post("/items").param("id", ball.getId().toString()).param("action", "MINUS"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1490")));

        mockMvc.perform(post("/cart/items").param("id", ball.getId().toString()).param("action", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Мяч футбольный"))));
    }

    private void addToCart(Item item) throws Exception {
        mockMvc.perform(post("/items").param("id", item.getId().toString()).param("action", "PLUS"))
                .andExpect(status().is3xxRedirection());
    }
}