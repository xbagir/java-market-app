# Витрина интернет-магазина (my-market-app)

## О проекте

Мультимодульное Maven-приложение (спринт 8): витрина товаров + RESTful-сервис платежей +
сервер авторизации OAuth2.

- **storefront** — веб-витрина: страницы каталога (поиск, сортировка, пагинация), товара,
  корзины, заказов и оформление покупки. Каталог кешируется в Redis (reactive).
  Покупка списывает стоимость с баланса через REST-сервис платежей (OpenAPI-генерация клиента).
  Вход покупателей через форму (**Spring Security**, session), корзина и заказы привязаны
  к аутентифицированному пользователю. К сервису платежей витрина ходит по
  **OAuth2 Client Credentials**.
- **payment-service** — реактивный REST-сервис платежей (OpenAPI): запрос баланса
  и списание средств. Баланс хранится в памяти (`ConcurrentHashMap`), счёт создаётся
  при первом обращении. Защищён как **OAuth2 Resource Server** (JWT, scope `payment.read`/`payment.write`).
- **auth-server** — сервер авторизации (**Spring Authorization Server**): выдаёт JWT
  по Client Credentials Flow, публикует JWKS.

## Технологический стек

- Java 21, Spring Boot 3.4, Spring WebFlux (reactive, Netty), Thymeleaf
- Spring Security (form login/logout у витрины), Spring Authorization Server,
  OAuth2 Client, OAuth2 Resource Server (JWT)
- Spring Data R2DBC — у витрины (H2 в памяти для тестов; PostgreSQL в Docker Compose)
- Spring Data Redis **Reactive** — кеш каталога витрины (TTL-кеш товаров и страниц)
- OpenAPI 3.0 + openapi-generator-maven-plugin 7.23.0:
  — генерация **сервера** (spring, reactive) для payment-service из `specs/payment-api.yaml`
  — генерация **клиента** (java, webclient) для storefront
- Тесты: JUnit 5, Spring Boot Test, WebTestClient, `@WebFluxTest`, `@DataR2dbcTest`,
  Spring Security Test (`mockUser`, `mockJwt`), Testcontainers (Redis), StepVerifier

## Архитектура безопасности

```
браузер ──форма логина──▶ storefront ──Bearer JWT──▶ payment-service
                              │                          ▲
                              └── token-uri (CC flow) ──▶ auth-server ── JWKS ──┘
```

- Покупатель регистрируется (`/register`) и входит по форме (`/login`). Пароли хранятся
  в БД в виде BCrypt-хешей. `/cart`, `/orders`, `/buy` и POST-изменения каталога доступны
  только аутентифицированным пользователям.
- Витрина получает access-токен у auth-server по **Client Credentials** и добавляет его
  к каждому вызову сервиса платежей.
- payment-service проверяет JWT по публичным ключам auth-server (`jwk-set-uri`) и требует
  scope `payment.read` для чтения баланса и `payment.write` для списания.
- Счёт покупателя в сервисе платежей — на каждого пользователя витрины
  (идентификатор пользователя из БД витрины передаётся как `accountId`).

OAuth2-клиенты (демонстрационные):

| Клиент | Секрет | Grant | Scopes |
| --- | --- | --- | --- |
| `storefront` | `storefront-secret` | client_credentials | `payment.read`, `payment.write` |
| `payment-client` | `payment-client-secret` | — (регистрация resource server) | `payment.read` |

## Сборка мультипроекта

Из корня репозитория (нужна Java 21 и Maven 3.9+):

```bash
mvn clean package
```

Сборка всех подпроектов в одноразовом контейнере Maven (без установки Maven на хост):

```bash
docker run --rm -v "$PWD":/app -w /app -v market-m2:/root/.m2 \
  maven:3.9-eclipse-temurin-21 mvn -B clean package
```

Результат — три Executable JAR: `storefront/target/storefront-0.0.1-SNAPSHOT.jar`,
`payment-service/target/payment-service-0.0.1-SNAPSHOT.jar` и
`auth-server/target/auth-server-0.0.1-SNAPSHOT.jar`.

## Запуск (только Docker)

`docker-compose.yml` поднимает пять сервисов: PostgreSQL, Redis, сервер авторизации,
витрину и сервис платежей.

```bash
docker compose up --build
```

- Витрина: <http://localhost:8080/> (`APP_PORT` для смены порта)
- Сервис платежей: <http://localhost:8081/health> (`PAYMENTS_PORT`)
- Сервер авторизации: <http://localhost:9000/health> (`AUTH_PORT`)
- Redis: `localhost:6379` (`REDIS_PORT`)

Баланс счёта покупателя = `INITIAL_BALANCE` (по умолчанию 100000) — хватает для тестовой покупки.

> **Обновление с прошлого спринта.** В схему БД добавлены таблицы `users` и колонки
> `user_id` в `cart_items`/`shop_orders`. Так как `schema.sql` использует
> `CREATE TABLE IF NOT EXISTS`, старая база из Docker-тома не мигрирует автоматически —
> пересоздайте том: `docker compose down -v && docker compose up --build`.

## Локальный запуск (без Docker Compose)

1. Поднимите локальный Redis (кеш каталога): `docker run -p 6379:6379 redis:7-alpine`
2. Сервер авторизации:
   `java -jar auth-server/target/auth-server-0.0.1-SNAPSHOT.jar` (порт 9000)
3. Сервис платежей:
   `java -jar payment-service/target/payment-service-0.0.1-SNAPSHOT.jar`
   (порт `SERVER_PORT`, по умолчанию 8081; JWKS — `AUTH_JWK_SET_URI`,
   по умолчанию `http://localhost:9000/oauth2/jwks`)
4. Витрина:
   `java -jar storefront/target/storefront-0.0.1-SNAPSHOT.jar` — H2 в памяти по умолчанию,
   Redis на `localhost:6379`, платежи на `http://localhost:8081`,
   token-uri auth-server — `AUTH_TOKEN_URI` (по умолчанию `http://localhost:9000/oauth2/token`).

Демо-пользователь создаётся при старте: **`user` / `password`** (настраивается через
`SEED_USERNAME`/`SEED_PASSWORD`).

## Сценарий использования

1. Откройте витрину <http://localhost:8080/> — каталог товаров (поиск, сортировка, пагинация).
2. Зарегистрируйтесь (`/register`) или войдите демо-пользователем `user`/`password`.
3. Добавьте товар кнопкой «🛒»/«+»; корзина и история заказов привязаны к вашему аккаунту.
4. В корзине видны товары, цены, сумма и **баланс счёта**; меняйте количество, удаляйте позиции.
5. Нажмите «Купить» — витрина запросит баланс в сервисе платежей, спишет сумму заказа
   и откроет страницу оформленного заказа; заказы — кнопка «Заказы».
6. Если средств мало или сервис платежей недоступен — кнопка «Купить» отключена,
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
| `app.payments.timeout` | `PAYMENTS_TIMEOUT` | `5s` | Тайм-аут ответа сервиса платежей |
| `...registration.payment-service.*` | `AUTH_TOKEN_URI` | `http://localhost:9000/oauth2/token` | Token-uri auth-server |
| `...payment-service.client-id` | `AUTH_STOREFRONT_CLIENT_ID` | `storefront` | OAuth2 client-id витрины |
| `...payment-service.client-secret` | `AUTH_STOREFRONT_CLIENT_SECRET` | `storefront-secret` | OAuth2 client-secret витрины |
| `app.seed.enabled` | `SEED_DATABASE` | `true` | Загружать демо-товары и пользователя |
| `app.seed.user.username` | `SEED_USERNAME` | `user` | Демо-пользователь витрины |
| `app.seed.user.password` | `SEED_PASSWORD` | `password` | Пароль демо-пользователя |
| `server.port` (payments) | `SERVER_PORT` | `8081` | Порт сервиса платежей |
| `app.payments.initial-balance` | `INITIAL_BALANCE` | `100000` | Стартовый баланс счёта покупателя |
| `...resourceserver.jwt.jwk-set-uri` | `AUTH_JWK_SET_URI` | `http://localhost:9000/oauth2/jwks` | JWKS auth-server |
| `server.port` (auth) | `AUTH_PORT` | `9000` | Порт auth-server |
| `app.auth.issuer` | `AUTH_SERVER_ISSUER` | `http://localhost:9000` | Issuer auth-server |
| `app.auth.storefront.*` | `STOREFRONT_CLIENT_ID`, `STOREFRONT_CLIENT_SECRET` | `storefront` | Клиент витрины |
| `app.auth.resource-server.*` | `RESOURCE_SERVER_CLIENT_ID`, `RESOURCE_SERVER_CLIENT_SECRET` | `payment-client` | Resource server |

## Эндпоинты

### Витрина

| Метод | Путь | Доступ | Параметры | Результат |
| --- | --- | --- | --- | --- |
| GET | `/`, `/items` | любой | `search`, `sort=NO\|ALPHA\|PRICE`, `pageNumber` (с 1), `pageSize` (2–100) | Шаблон `items` |
| GET | `/items/{id}` | любой | — | Шаблон `item` |
| GET | `/login`, `/register` | любой | — | Формы входа/регистрации |
| POST | `/register` | любой | `username`, `password` (форма) | `redirect:/login?registered` или форма с ошибкой |
| POST | `/login` | любой | `username`, `password` (форма) | Устанавливает сессию |
| POST | `/logout` | вход выполнен | — | Очищает сессию, `redirect:/login?logout` |
| POST | `/items?id&action` | вход выполнен | `id`, `action=PLUS\|MINUS`, параметры каталога | Редирект на каталог |
| POST | `/items/{id}` | вход выполнен | `action=PLUS\|MINUS` | Редирект на товар |
| GET | `/cart`, `/cart/items` | вход выполнен | — | Шаблон `cart`: товары, `total`, видимый **баланс** |
| POST | `/cart/items` | вход выполнен | `id`, `action=PLUS\|MINUS\|DELETE` | Редирект на корзину |
| GET | `/orders` | вход выполнен | — | Заказы текущего пользователя (новые сверху) |
| GET | `/orders/{id}` | вход выполнен | `newOrder=true` | Заказ: состав, сумма, флаг `newOrder` |
| POST | `/buy` | вход выполнен | — (пустая корзина → 400) | Списывает баланс, редирект `redirect:/orders/{id}?newOrder=true` |

Анонимные GET-запросы защищённых страниц редиректятся на `/login`. Покупка: витрина
проверяет баланс (GET account) и списывает сумму заказа (POST payment) через `PaymentGateway`.
Недостаточно средств → **402**; сервис недоступен → **503**; витрина не авторизована
в сервисе платежей → **502**.

### Сервис платежей (REST, OpenAPI `specs/payment-api.yaml`)

Все эндпоинты (кроме `/health`) требуют JWT.

| Метод | Путь | Scope | Результат |
| --- | --- | --- | --- |
| GET | `/health` | — | `200` — `{"status":"UP"}` |
| GET | `/api/v1/accounts/{accountId}` | `payment.read` | `200` — баланс (`AccountResponse`); `400` — неверный id |
| POST | `/api/v1/payments` | `payment.write` | `201` — списание (`PaymentResponse`); `400` — неверный запрос; `409` — недостаточно средств |

Счёт создаётся при первом обращении с балансом `INITIAL_BALANCE`.
Ошибки — в формате `application/problem+json` (RFC 9457).

### Сервер авторизации

| Метод | Путь | Результат |
| --- | --- | --- |
| GET | `/health` | `200` — `{"status":"UP"}` |
| POST | `/oauth2/token` | Выдача токена (Client Credentials) |
| GET | `/oauth2/jwks` | Публичные ключи для проверки JWT |

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

- `storefront`:
  - unit: `ItemServiceTest`, `CartServiceTest`, `OrderServiceTest`,
    `PaymentServiceClientTest` (контракт клиента против JDK `HttpServer`, включая 401 → 502);
  - срезы: `MarketControllerTest`, `CartControllerTest`, `OrderControllerTest`,
    `AuthControllerTest` (`@WebFluxTest` + `@Import(SecurityConfig.class)`, `mockUser`);
    `ItemRepositoryTest` (`@DataR2dbcTest`);
  - интеграционные: `MyMarketAppApplicationTests`, `ItemCacheTest`, `PurchasePaymentTest`
    (`@SpringBootTest`, `mockUser`, проверка привязки корзины/заказов к пользователю).
- `payment-service`: `AccountServiceTest` (unit, автосоздание счёта) и
  `PaymentServiceApplicationTest` (e2e WebTestClient, `mockJwt`, проверка 401/403 и scope).
- `auth-server`: `AuthorizationServerTest` (токен по Client Credentials, неверный секрет → 401,
  `/health` публичный).

### Стратегия кеширования тестовых контекстов

Полные контексты (`@SpringBootTest`) сведены к **одному** случаю: классы
`MyMarketAppApplicationTests`, `ItemCacheTest` и `PurchasePaymentTest` используют
идентичные аннотации `@SpringBootTest + @AutoConfigureWebTestClient +
@Import({RedisTestConfig.class, StubPaymentConfig.class})` — Spring кеширует один контекст,
Redis-контейнер Testcontainers стартует один раз. Срезовые контексты (`@WebFluxTest`,
`@DataR2dbcTest`) и unit-тесты не добавляют полноценных контекстов.

## Структура проекта

```
├── pom.xml                        # parent: модули storefront + payment-service + auth-server
├── specs/payment-api.yaml         # единая OpenAPI-схема (генерация сервера и клиента)
├── storefront/
│   ├── Dockerfile
│   └── src/main/java/ru/yandex/practicum/mymarket/
│       ├── MyMarketAppApplication.java
│       ├── controller/            # MarketController, CartController, OrderController,
│       │                          #   AuthController, GlobalExceptionHandler
│       ├── service/               # ItemService, CartService, OrderService,
│       │                          #   UserService, PaymentGateway, PaymentServiceClient
│       ├── repository/ model/ dto/ exception/
│       ├── security/              # CurrentUserProvider
│       └── config/                # SecurityConfig, OAuth2ClientConfig, DataInitializer,
│                                  #   PaymentProperties, PaymentClientConfig
│   └── src/main/resources/        # schema.sql, templates/*, templates/error/*.html
├── payment-service/
│   ├── Dockerfile
│   └── src/main/java/ru/yandex/practicum/payment/
│       ├── PaymentServiceApplication.java
│       ├── service/               # AccountService (баланс/списание в памяти, автосоздание счёта)
│       ├── controller/            # PaymentController, PaymentExceptionHandler, HealthController
│       └── config/                # SecurityConfig (Resource Server)
└── auth-server/
    ├── Dockerfile
    └── src/main/java/ru/yandex/practicum/authserver/
        ├── AuthServerApplication.java
        ├── config/                # AuthorizationServerConfig (clients, RSA, JWKS)
        └── web/                   # HealthController
```

Кеш витрины: `ItemCache` (reactive, ключи `storefront:item:{id}` и `storefront:page:...`,
прозрачный fallback в БД при сбое кеша); витрина и корзина читают товары через этот кеш.
Платежи: `PaymentServiceClient` на сгенерированном `ApiClient`/`AccountsApi`/`PaymentsApi`
с OAuth2-фильтром (`ServerOAuth2AuthorizedClientExchangeFilterFunction`), тайм-аутом ответа
(`app.payments.timeout`), маппингом ошибок: `409` → `InsufficientFundsException` (402),
`401/403` → `PaymentUnauthorizedException` (502), недоступность или тайм-аут →
`PaymentUnavailableException` (503).
