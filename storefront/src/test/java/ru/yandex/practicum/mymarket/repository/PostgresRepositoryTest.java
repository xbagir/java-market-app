package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.r2dbc.spi.ConnectionFactory;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void r2dbcProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://"
                + postgres.getHost() + ":" + postgres.getMappedPort(5432)
                + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void initSchema() {
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql"))
                .populate(connectionFactory)
                .block();
    }

    @Test
    void schemaSupportsItemsUsersAndCart() {
        Item item = itemRepository
                .save(new Item("Мяч", "Описание", "images/ball.svg", 1490)).block();
        User user = userRepository
                .save(new User("pg_user", "hash", true, "ROLE_USER")).block();

        assertThat(item).isNotNull();
        assertThat(user).isNotNull();

        CartItem cartItem = cartItemRepository
                .save(new CartItem(user.getId(), item.getId(), 1)).block();
        assertThat(cartItem).isNotNull();
        assertThat(cartItemRepository.findByUserIdAndItemId(user.getId(), item.getId()).block())
                .isNotNull();
    }

    @Test
    void atomicIncrementAndDecrementUpdateQuantity() {
        Item item = itemRepository
                .save(new Item("Кукла", "Описание", "images/doll.svg", 1890)).block();
        User user = userRepository
                .save(new User("pg_user2", "hash", true, "ROLE_USER")).block();
        cartItemRepository.save(new CartItem(user.getId(), item.getId(), 1)).block();

        assertThat(cartItemRepository.incrementQuantity(user.getId(), item.getId()).block())
                .isEqualTo(1);
        assertThat(cartItemRepository.findByUserIdAndItemId(user.getId(), item.getId())
                .block().getQuantity()).isEqualTo(2);

        assertThat(cartItemRepository.decrementQuantity(user.getId(), item.getId()).block())
                .isEqualTo(1);
    }

    @Test
    void uniqueConstraintRejectsDuplicateCartItem() {
        Item item = itemRepository
                .save(new Item("Робот", "Описание", "images/robot.svg", 3490)).block();
        User user = userRepository
                .save(new User("pg_user3", "hash", true, "ROLE_USER")).block();
        cartItemRepository.save(new CartItem(user.getId(), item.getId(), 1)).block();

        assertThatThrownBy(() -> cartItemRepository
                .save(new CartItem(user.getId(), item.getId(), 1)).block())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
