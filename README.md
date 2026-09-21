# Витрина интернет-магазина (my-market-app)

## О проекте

Веб-приложение — витрина товаров, которые можно положить в корзину и купить.
Состоит из шести частей: страницы витрины товаров (поиск, сортировка, пагинация),
страницы товара, корзины покупателя, страницы всех заказов, страницы заказа
и сервиса покупки (без отдельной веб-страницы, оформляет заказ из корзины).

## Технологический стек

- Java 21, Spring Boot 3.4, Spring Web MVC (блокирующий стек), Thymeleaf
- Spring Data JPA, Hibernate ORM
- База данных: PostgreSQL в Docker Compose; H2 (в памяти) — только для dev-профиля вне Docker (задаётся через `SPRING_DATASOURCE_*`)
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
├── service/                      # бизнес-логика: ItemService, CartService, OrderService
├── repository/                   # Spring Data JPA репозитории
├── model/                        # сущности: Item, CartItem, Order, OrderItem
├── dto/                          # record-модели: ItemDto, OrderDto, CartView, ItemQuantity, Paging, Action, SortOption
├── exception/                    # NotFoundException (404), EmptyCartException (400)
└── config/                       # DataInitializer — загрузка демо-товаров в пустую витрину

src/main/resources/
├── templates/                    # Thymeleaf-шаблоны (items, item, cart, orders, order, error/*)
└── static/images/                # изображения товаров (SVG)
```

## Эндпоинты

| Метод | Путь | Параметры | Результат |
| --- | --- | --- | --- |
| GET | `/`, `/items` | `search` (поиск по названию/описанию), `sort=NO\|ALPHA\|PRICE`, `pageNumber` (с 1), `pageSize` (2–100) | Шаблон `items`: сетка товаров по 3, `search`, `sort`, `paging` |
| POST | `/items` | `id`, `action=PLUS\|MINUS`, плюс `search`, `sort`, `pageNumber`, `pageSize` | Редирект `redirect:/items?...` с теми же параметрами |
| GET | `/items/{id}` | — | Шаблон `item`: товар + количество в корзине |
| POST | `/items/{id}` | `action=PLUS\|MINUS` | Редирект `redirect:/items/{id}` |
| GET | `/cart`, `/cart/items` | — | Шаблон `cart`: товары в корзине + `total` |
| POST | `/cart/items` | `id`, `action=PLUS\|MINUS\|DELETE` | Редирект `redirect:/cart/items` |
| GET | `/orders` | — | Шаблон `orders`: заказы (новые сверху) |
| GET | `/orders/{id}` | `newOrder=true`, если переход после покупки | Шаблон `order`: состав, сумма, флаг `newOrder` |
| POST | `/buy` | — (корзина не должна быть пуста, иначе 400) | Редирект `redirect:/orders/{id}?newOrder=true` |

## Тесты

Тесты запускаются в одноразовом контейнере Maven (без установки на хост):

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-21 mvn test
```

- `repository/ItemRepositoryTest` — доступ к данным (`@DataJpaTest`)
- `service/*Test` — юнит-тесты сервисов (Mockito)
- `controller/*Test` — тесты веб-слоя (`@WebMvcTest` + MockMvc)
- `MyMarketAppApplicationTests` — интеграционные тесты полного потока покупки (`@SpringBootTest` + `@AutoConfigureMockMvc`)