# Витрина интернет-магазина (my-market-app)

## Технологический стек

- Java 21, Spring Boot 3.4, Spring Web MVC (блокирующий стек), Thymeleaf
- Spring Data JPA, Hibernate ORM
- База данных: по умолчанию H2 (в памяти), поддерживается PostgreSQL (задаётся через `SPRING_DATASOURCE_*`)
- Сборка: Maven (используется Java 21)
- Тесты: JUnit 5, Spring Boot Test, MockMvc, `@WebMvcTest`, `@DataJpaTest`, контексты кешируются

## Запуск (только Docker)
```bash
docker compose up --build
```

Приложение будет доступно по адресу <http://localhost:8080/>.

## Настройки (`application.properties`)

| Свойство | Переменная окружения | По умолчанию | Описание |
| --- | --- | --- | --- |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:market;DB_CLOSE_DELAY=-1` | JDBC URL |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `sa` | Пользователь БД |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | *(пусто)* | Пароль БД |
| `app.seed.enabled` | `SEED_DATABASE` | `true` | Загружать демо-товары |
| `server.port` | `APP_PORT` | `8080` | Порт HTTP |

Для PostgreSQL: `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/market`,
`SPRING_DATASOURCE_DRIVER=org.postgresql.Driver` и учётные данные БД.

## Структура проекта

```
src/main/java/ru/yandex/practicum/mymarket/
├── MyMarketAppApplication.java   # точка входа
├── controller/                   # веб-слой: MarketController, CartController, OrderController
├── service/                      # бизнес-логика: ItemService, CartService, OrderService, DataInitializer
├── repository/                   # Spring Data JPA репозитории
├── model/                        # сущности: Item, CartItem, Order, OrderItem
├── dto/                          # record-модели для шаблонов: ItemDto, OrderDto, Paging, Action, SortOption
└── exception/                    # NotFoundException

src/main/resources/
├── templates/                    # Thymeleaf-шаблоны (items, item, cart, orders, order, error/*)
└── static/images/                # изображения товаров (SVG)
```

## Эндпоинты

| Метод | Путь | Описание |
| --- | --- | --- |
| GET | `/`, `/items` | Витрина (search, sort, pageNumber, pageSize) |
| GET | `/items/{id}` | Карточка товара |
| POST | `/items` | Изменить количество в корзине с витрины (`action=PLUS\|MINUS`) |
| POST | `/items/{id}` | Изменить количество в корзине с карточки товара |
| GET | `/cart`, `/cart/items` | Корзина |
| POST | `/cart/items` | Изменить количество / удалить товар (`action=PLUS\|MINUS\|DELETE`) |
| GET | `/orders` | Список заказов |
| GET | `/orders/{id}` | Страница заказа (`newOrder=true` — покупка совершена) |
| POST | `/buy` | Оформить заказ из корзины |

## Тесты

Тесты запускаются в одноразовом контейнере Maven (без установки на хост):

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-21 mvn test
```

- `repository/ItemRepositoryTest` — доступ к данным (`@DataJpaTest`)
- `service/*Test` — юнит-тесты сервисов (Mockito)
- `controller/*Test` — тесты веб-слоя (`@WebMvcTest` + MockMvc)
- `MyMarketAppApplicationTests` — интеграционные тесты полного потока покупки (`@SpringBootTest` + `@AutoConfigureMockMvc`)