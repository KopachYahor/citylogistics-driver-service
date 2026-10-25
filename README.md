# CityLogistics — Driver Service

Микросервис для управления водителями и их статусами. Общается с монолитом (`citylogistics-core`) исключительно через Apache Kafka.

## Роль в системе
- **Слушает `order.created`:** Находит свободного водителя (`AVAILABLE`), переводит в `BUSY` и публикует `driver.assigned`.
- **Слушает `order.status-changed`:** Если заказ доставлен (`DELIVERED`), освобождает водителя (переводит обратно в `AVAILABLE`).

## Стек
- Java 21, Spring Boot 3.3
- Spring Kafka (`@KafkaListener`)
- Spring Data JPA, PostgreSQL 15, Flyway
- **Своя БД `drivers`** на порту `5433` (паттерн Database per Service).

## Ключевые фичи
- **Идемпотентность:** `OrderCreatedListener` проверяет `existsByOrderId`. Повторная доставка сообщения из Kafka не приведет к двойному назначению водителя.
- **Транзакции:** Все изменения в слушателях обернуты в `@Transactional`.

## Запуск
1. Сначала поднимите инфраструктуру из репы `core`: `cd ../citylogistics-core && docker compose up -d`
2. Запустите сервис: `./mvnw spring-boot:run` (порт 8082).