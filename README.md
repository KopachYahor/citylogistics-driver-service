# CityLogistics — Driver Service

Сервис водителей проекта CityLogistics: хранит водителей, назначает свободного водителя
на новый заказ и освобождает его, когда заказ доставлен или отменён.

Общее описание проекта, схема и полная инструкция запуска — в README
[citylogistics-core](../citylogistics-core). Инфраструктура (Kafka, обе базы) поднимается
оттуда же.

## Роль в системе

| Как общается | Что происходит |
|---|---|
| Kafka: читает `order.created` | Находит водителя `AVAILABLE`, создаёт запись в `order_assignments`, ставит водителю `BUSY`, публикует `driver.assigned` |
| Kafka: читает `order.status-changed` | При `DELIVERED` или `CANCELLED` снова делает водителя `AVAILABLE` |
| REST: `POST /api/drivers` | core вызывает его при регистрации пользователя с ролью `DRIVER` |

Группа консьюмеров: `driver-service-group`. Ключ сообщений — id заказа.

## Стек

Java 21, Spring Boot 3.3, Spring Kafka (`@KafkaListener`), Spring Data JPA, PostgreSQL 15,
Flyway (V1–V3), JUnit 5, Mockito, Testcontainers. Своя база `drivers` на порту 5433
(Database per Service).

## REST API

| Метод и адрес | Что делает |
|---|---|
| `GET /api/drivers` | Все водители |
| `GET /api/drivers/available` | Свободные водители |
| `GET /api/drivers/{id}` | Водитель по id |
| `POST /api/drivers` | Создать водителя (ответ `201`): `userId`, `name`, `phone` |

Каждый запрос должен нести заголовок `X-Internal-Api-Key` с общим ключом
(`InternalApiKeyFilter`), иначе ответ `401 {"error":"Unauthorized"}`. Ключ задаётся
переменной окружения `INTERNAL_API_KEY`, одинаковой у core и driver-service. Пример:

```
curl localhost:8082/api/drivers -H "X-Internal-Api-Key: <общий_ключ>"
```

## Ключевые решения

- **Идемпотентность назначения.** Kafka доставляет «хотя бы один раз», поэтому
  `OrderCreatedListener` сначала проверяет `existsByOrderId`. Повтор события не создаёт
  второе назначение. У топика одна партиция, поэтому в группе его читает только один
  экземпляр сервиса. Но при перебалансировке (экземпляр завис или перезапустился)
  то же сообщение может обрабатываться повторно, пока первая обработка ещё не закончилась.
  На этот случай стоит `UNIQUE` на `order_assignments.order_id` (миграция V3): вторая
  вставка не пройдёт, транзакция откатится, а при повторной попытке сработает проверка.
- **Освобождается только текущий заказ водителя.** `OrderStatusChangedListener` сверяет
  заказ из события с последним назначением водителя. Старое повторное событие не освободит
  водителя, который уже везёт другой заказ.
- **Транзакции.** Изменения в слушателях обёрнуты в `@Transactional`: назначение
  и смена статуса водителя сохраняются вместе или не сохраняются вовсе.
- **Битые сообщения не блокируют чтение.** `ErrorHandlingDeserializer` логирует и
  пропускает сообщение, которое не разбирается как JSON.
- **`auto-offset-reset: earliest`.** При самом первом запуске сервис прочитает и заказы,
  созданные до его старта.

## Запуск

1. Поднять инфраструктуру из core: `cd ../citylogistics-core && docker compose up -d`.
2. Запустить сервис (порт 8082) с тем же общим ключом, что у core:
   ```
   $env:INTERNAL_API_KEY="<общий_ключ>"
   .\mvnw.cmd spring-boot:run
   ```
   На macOS/Linux: `export INTERNAL_API_KEY=...` и `./mvnw spring-boot:run`.
   Как сгенерировать ключ — в README core.

Порядок запуска core и driver-service не важен: события подождут в Kafka. Но для
регистрации водителя driver-service должен уже работать.

## Тесты

```
.\mvnw.cmd test
```

Нужен запущенный Docker. Тест запуска приложения поднимает временную PostgreSQL
(Testcontainers) и проверяет миграции и соответствие сущностей схеме. Слушатели Kafka
покрыты unit-тестами на Mockito (`OrderCreatedListenerTest`, `OrderStatusChangedListenerTest`),
защита REST — тестом на MockMvc (`DriverControllerApiKeyTest`).
Рабочая база на 5433 не используется, `INTERNAL_API_KEY` для тестов не нужен.

## Известные ограничения

1. **Защита REST простая.** Один общий ключ без срока жизни и ролей; при утечке его
   нужно сменить вручную в обоих сервисах. В продакшене обычно используют токены
   сервисов (OAuth2 client credentials) или mTLS.
2. **Нет повторной попытки назначения.** Если свободного водителя нет, заказ остаётся
   `NEW`, и к нему никто не возвращается.
3. **Двойная запись.** Событие `driver.assigned` отправляется в Kafka до коммита транзакции,
   результат отправки не проверяется. При сбое Kafka core не узнает о назначении.
4. **DTO событий продублированы.** `OrderCreatedEvent` и `OrderStatusChangedEvent` лежат
   в пакете `com.kopach.logistics.order.dto`, как в core: `JsonSerializer` передаёт полное
   имя класса в заголовке `__TypeId__`, и оно должно совпадать в обоих сервисах.
