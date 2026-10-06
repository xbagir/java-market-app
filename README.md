# Витрина интернет-магазина (my-market-app)

## О проекте

Веб-приложение — витрина товаров, которые можно положить в корзину и купить.
Состоит из шести частей: страницы витрины товаров (поиск, сортировка, пагинация),
страницы товара, корзины покупателя, страницы всех заказов, страницы заказа
и сервиса покупки (без отдельной веб-страницы, оформляет заказ из корзины).

## Технологический стек

- Java 21, Spring Boot 3.4, Spring WebFlux (реактивный стек, Netty), Thymeleaf
- Spring Data R2DBC (связи сущностей — вручную через FK-ключи, схема в `schema.sql`)
- База данных: H2 (в памяти) по умолчанию и для тестов; PostgreSQL в Docker Compose — через `SPRING_R2DBC_*`
- Сборка: Maven (используется Java 21)
- Тесты: JUnit 5, Spring Boot Test, WebTestClient, `@WebFluxTest`, `@DataR2dbcTest`, StepVerifier, контексты кешируются

## Запуск (только Docker)
```bash
docker compose up --build
```

Приложение будет доступно по адресу <http://localhost:8080/>.

## Настройки (`application.properties`)

| Свойство | Переменная окружения | По умолчанию | Описание |
| --- | --- | --- | --- |
| `spring.r2dbc.url` | `SPRING_R2DBC_URL` | `r2dbc:h2:mem:///market` | R2DBC URL |
| `spring.r2dbc.username` | `SPRING_R2DBC_USERNAME` | `sa` | Пользователь БД |
| `spring.r2dbc.password` | `SPRING_R2DBC_PASSWORD` | *(пусто)* | Пароль БД |
| `app.seed.enabled` | `SEED_DATABASE` | `true` | Загружать демо-товары |
| `server.port` | `APP_PORT` | `8080` | Порт HTTP |

Для PostgreSQL: `SPRING_R2DBC_URL=r2dbc:postgresql://localhost:5432/market`
и учётные данные БД (`SPRING_R2DBC_USERNAME`, `SPRING_R2DBC_PASSWORD`).
H2-консоль недоступна: приложение использует только реактивный драйвер, без JDBC.

## Структура проекта

```
src/main/java/ru/yandex/practicum/mymarket/
├── MyMarketAppApplication.java   # точка входа
├── controller/                   # веб-слой: MarketController, CartController, OrderController
├── service/                      # бизнес-логика: ItemService, CartService, OrderService
├── repository/                   # Spring Data R2DBC репозитории (+ OrderItemRepository)
├── model/                        # сущности без связей: Item, CartItem, Order, OrderItem
├── dto/                          # record-модели: ItemDto, OrderDto, CartView, Paging, Action, SortOption
├── exception/                    # NotFoundException (404), EmptyCartException (400)
└── config/                       # DataInitializer — загрузка демо-товаров в пустую витрину

src/main/resources/
├── schema.sql                      # DDL для H2 и PostgreSQL (R2DBC не умеет ddl-auto)
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
docker run --rm -v "$PWD":/app -w /app -v market-m2:/root/.m2 maven:3.9-eclipse-temurin-21 mvn test
```

- `repository/ItemRepositoryTest` — доступ к данным (`@DataR2dbcTest`, StepVerifier)
- `service/*Test` — юнит-тесты сервисов (Mockito + StepVerifier)
- `controller/*Test` — тесты веб-слоя (`@WebFluxTest` + WebTestClient)
- `MyMarketAppApplicationTests` — интеграционные тесты полного потока покупки (`@SpringBootTest` + WebTestClient)