# Витрина интернет-магазина (my-market-app)

## О проекте

Мультимодульное Maven-приложение (спринт 7): витрина товаров + RESTful-сервис платежей.

- **storefront** — веб-витрина: страницы каталога (поиск, сортировка, пагинация), товара,
  корзины, заказов и оформление покупки. Каталог кешируется в Redis (reactive).
  Покупка списывает стоимость с баланса через REST-сервис платежей (OpenAPI-генерация клиента).
- **payment-service** — реактивный REST-сервис платежей (OpenAPI): запрос баланса
  и списание средств. Реализация — в памяти (`ConcurrentHashMap`), схема БД не нужна.

## Технологический стек

- Java 21, Spring Boot 3.4, Spring WebFlux (reactive, Netty), Thymeleaf
- Spring Data R2DBC — у витрины (H2 в памяти для тестов; PostgreSQL в Docker Compose)
- Spring Data Redis **Reactive** — кеш каталога витрины (TTL-кеш товаров и страниц)
- OpenAPI 3.0 + openapi-generator-maven-plugin 7.23.0:
  — генерация **сервера** (spring, reactive) для payment-service из `specs/payment-api.yaml`
  — генерация **клиента** (java, webclient) для storefront
- Тесты: JUnit 5, Spring Boot Test, WebTestClient, `@WebFluxTest`, `@DataR2dbcTest`,
  Testcontainers (Redis), StepVerifier; кеширование контекстов

## Сборка мультипроекта

Из корня репозитория (нужна Java 21 и Maven 3.9+):

```bash
mvn clean package
```

Сборка обоих подпроектов в одноразовом контейнере Maven (без установки Maven на хост):

```bash
docker run --rm -v "$PWD":/app -w /app -v market-m2:/root/.m2 \
  maven:3.9-eclipse-temurin-21 mvn -B clean package
```

Результат — два Executable JAR: `storefront/target/storefront-0.0.1-SNAPSHOT.jar`
и `payment-service/target/payment-service-0.0.1-SNAPSHOT.jar`.

## Запуск (только Docker)

`docker-compose.yml` поднимает четыре сервиса: PostgreSQL, Redis, витрину и сервис платежей.

```bash
docker compose up --build
```

- Витрина: <http://localhost:8080/> (`APP_PORT` для смены порта)
- Сервис платежей: <http://localhost:8081/api/v1/accounts/1> (`PAYMENTS_PORT`)
- Redis: `localhost:6379` (`REDIS_PORT`)

Баланс демо-счёта = `INITIAL_BALANCE` (по умолчанию 100000) — хватает для тестовой покупки.

## Локальный запуск (без Docker Compose)

1. Поднимите локальный Redis (кеш каталога): `docker run -p 6379:6379 redis:7-alpine`
2. Сервис платежей — из собранного JAR:
   `java -jar payment-service/target/payment-service-0.0.1-SNAPSHOT.jar`
   (или запуском класса `PaymentServiceApplication` из IDE; порт `SERVER_PORT`, по умолчанию 8081)
3. Витрина — из собранного JAR:
   `java -jar storefront/target/storefront-0.0.1-SNAPSHOT.jar`
   (или класс `MyMarketAppApplication` из IDE) — H2 в памяти по умолчанию,
   Redis на `localhost:6379`, платежи на `http://localhost:8081`.

## Сценарий использования

1. Откройте витрину <http://localhost:8080/> — каталог товаров (поиск, сортировка, пагинация).
2. Добавьте товар кнопкой «🛒» или «+» — откроется страница товара, затем переходите в корзину.
3. В корзине видны товары, цены, сумма и **баланс счёта**; меняйте количество, удаляйте позиции.
4. Нажмите «Купить» — витрина запросит баланс в сервисе платежей, спишет сумму заказа
   и откроет страницу оформленного заказа; заказы — кнопка «Заказы».
5. Если средств мало или сервис платежей недоступен — кнопка «Купить» отключена,
   на странице корзины показано сообщение; при прямом POST `/buy` вернётся 402/503.

## Настройки

| Свойство | Переменная окружения | По умолчанию | Описание |
| --- | --- | --- | --- |
| `server.port` | `APP_PORT` | `8080` | Порт витрины |
| `spring.r2dbc.url` | `SPRING_R2DBC_URL` | `r2dbc:h2:mem:///market` | БД витрины |
| `spring.data.redis.host` | `REDIS_HOST` | `localhost` | Redis |
| `spring.data.redis.port` | `REDIS_PORT` | `6379` | Redis |
| `app.cache.item-ttl` | `CACHE_ITEM_TTL` | `10m` | TTL кеша товаров и страниц |
| `app.payments.base-url` | `PAYMENTS_BASE_URL` | `http://localhost:8081` | Базовый URL платежей |
| `app.payments.account-id` | `PAYMENTS_ACCOUNT_ID` | `1` | Демо-счёт покупателя |
| `app.seed.enabled` | `SEED_DATABASE` | `true` | Загружать демо-товары |
| `server.port` (payments) | `SERVER_PORT` | `8081` | Порт сервиса платежей |
| `app.payments.initial-balance` | `INITIAL_BALANCE` | `100000` | Стартовый баланс счёта |

## Эндпоинты

### Витрина

| Метод | Путь | Параметры | Результат |
| --- | --- | --- | --- |
| GET | `/`, `/items` | `search`, `sort=NO\|ALPHA\|PRICE`, `pageNumber` (с 1), `pageSize` (2–100) | Шаблон `items` |
| POST | `/items?id&action` | `id`, `action=PLUS\|MINUS`, плюс параметры каталога | Редирект на каталог |
| GET | `/items/{id}` | — | Шаблон `item` |
| POST | `/items/{id}` | `action=PLUS\|MINUS` | Редирект на товар |
| GET | `/cart`, `/cart/items` | — | Шаблон `cart`: товары, `total`, видимый **баланс** |
| POST | `/cart/items` | `id`, `action=PLUS\|MINUS\|DELETE` | Редирект на корзину |
| GET | `/orders` | — | Список заказов (новые сверху) |
| GET | `/orders/{id}` | `newOrder=true` | Заказ: состав, сумма, флаг `newOrder` |
| POST | `/buy` | — (пустая корзина → 400) | Списывает баланс, редирект `redirect:/orders/{id}?newOrder=true` |

Покупка: витрина проверяет баланс (GET account) и списывает сумму заказа (POST payment)
через `PaymentGateway`. Недостаточно средств → **402**; сервис недоступен → **503**.

### Сервис платежей (REST, OpenAPI `specs/payment-api.yaml`)

| Метод | Путь | Результат |
| --- | --- | --- |
| GET | `/api/v1/accounts/{accountId}` | `200` — баланс (`AccountResponse`); `404` — счёт не найден |
| POST | `/api/v1/payments` | `201` — списание (`PaymentResponse`); `400` — неверный запрос; `404` — счёт отсутствует; `409` — недостаточно средств |

Ошибки — в формате `application/problem+json` (RFC 9457).

## Тесты

Тесты запускаются в одноразовом контейнере Maven (без установки на хост).
Testcontainers (Redis) поднимается через docker.sock, поэтому контейнеру нужен доступ к нему:

```bash
docker run --rm --network host \
  -v "$PWD":/app -w /app \
  -v market-m2:/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  maven:3.9-eclipse-temurin-21 mvn test
```

- `storefront`: `ItemServiceTest`, `CartServiceTest`, `OrderServiceTest`, `PaymentServiceClientTest`
  (контрактный тест клиента против JDK `HttpServer`) — unit-тесты;
  `MarketControllerTest`, `CartControllerTest`, `OrderControllerTest` — `@WebFluxTest`;
  `ItemRepositoryTest` — `@DataR2dbcTest`;
  `MyMarketAppApplicationTests`, `ItemCacheTest`, `PurchasePaymentTest` — интеграционные (`@SpringBootTest`).
- `payment-service`: `AccountServiceTest` (unit) и `PaymentServiceApplicationTest` (e2e WebTestClient).

### Стратегия кеширования тестовых контекстов

Полные контексты (`@SpringBootTest`) сведены к **одному** случаю: классы
`MyMarketAppApplicationTests`, `ItemCacheTest` и `PurchasePaymentTest` используют
идентичные аннотации `@SpringBootTest + @AutoConfigureWebTestClient +
@Import({RedisTestConfig.class, StubPaymentConfig.class})` — Spring кеширует один контекст,
Redis-контейнер Testcontainers стартует один раз. Срезовые контексты (`@WebFluxTest`,
`@DataR2dbcTest`) и unit-тесты не добавляют полноценных контекстов.

## Структура проекта

```
├── pom.xml                        # parent: модули storefront + payment-service
├── specs/payment-api.yaml         # единая OpenAPI-схема (генерация сервера и клиента)
├── storefront/
│   ├── Dockerfile
│   └── src/main/java/ru/yandex/practicum/mymarket/
│       ├── MyMarketAppApplication.java
│       ├── controller/            # MarketController, CartController, OrderController, GlobalExceptionHandler
│       ├── service/               # ItemService, CartService, OrderService, PaymentGateway, PaymentServiceClient
│       ├── repository/ model/ dto/ exception/
│       └── config/                # DataInitializer, PaymentProperties, PaymentClientConfig
│   └── src/main/resources/        # schema.sql, templates/*, templates/error/{400,402,404,503,500}.html
└── payment-service/
    ├── Dockerfile
    └── src/main/java/ru/yandex/practicum/payment/
        ├── PaymentServiceApplication.java
        ├── service/               # AccountService (баланс/списание в памяти)
        ├── controller/            # PaymentController, PaymentExceptionHandler
        └── model/                 # AccountResponse, PaymentRequest, PaymentResponse, ProblemDetail
```

Кеш витрины: `ItemCache` (reactive, ключи `storefront:item:{id}` и `storefront:page:...`,
прозрачный fallback в БД при сбое кеша). Платежи: `PaymentServiceClient` на сгенерированном
`ApiClient`/`AccountsApi`/`PaymentsApi`, маппинг ошибок: `409` → `InsufficientFundsException`
(402), недоступный сервис → `PaymentUnavailableException` (503).