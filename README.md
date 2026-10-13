# CityLogistics — Driver Service

Микросервис водителей для платформы CityLogistics. Слушает Kafka-топик `order.created`,
назначает свободного водителя и публикует событие `driver.assigned`.

## Роль в системе

```
order.created ──▶ [driver-service] ──▶ driver.assigned
                  - слушает новые заказы
                  - ищет свободного водителя
                  - создаёт OrderAssignment
                  - ставит Driver.status=BUSY
                  - публикует событие
```

## Стек

- Java 21
- Spring Boot 3.3
- Spring Kafka
- Spring Data JPA + Hibernate
- PostgreSQL 15
- Flyway
- Maven

Своя БД `drivers` на порту **5433** (отдельная от монолита).

## Как запустить

1. Сначала поднять инфраструктуру из монолита:
   ```bash
   cd ../citylogistics-core
   docker compose up -d
   ```
2. Потом — этот сервис:
   ```bash
   ./mvnw spring-boot:run
   ```

Открывается `http://localhost:8082`.

## Kafka

- **Слушает:** `order.created` (группа `driver-service-group`, `auto-offset-reset: latest`)
- **Публикует:** `driver.assigned`
- **Идемпотентность:** `OrderCreatedListener` проверяет `existsByOrderId` — если заказ уже назначен, второго водителя не берёт
- **Транзакции:** `@Transactional` на `handleOrderCreated` — если `driverRepository.save` упадёт, `assignmentRepository.save` откатится

## REST API

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/drivers` | Все водители |
| GET | `/api/drivers/available` | Свободные (`status=AVAILABLE`) |
| GET | `/api/drivers/{id}` | Водитель по id |
| POST | `/api/drivers` | Создать водителя |

### Пример

```bash
curl -X POST http://localhost:8082/api/drivers \
  -H "Content-Type: application/json" \
  -d '{"name":"Иван Иванов","phone":"+375291234567"}'
```

## Основные классы

```
src/main/java/com/kopach/logistics/driver/
├── controller/DriverController
├── service/       — DriverService
├── kafka/
│   ├── OrderCreatedListener      ← @KafkaListener(order.created, group=driver-service-group)
│   └── DriverAssignedPublisher   ← KafkaTemplate.send(driver.assigned, orderId, event)
├── repository/    — DriverRepository, OrderAssignmentRepository
├── entity/        — Driver, DriverStatus, OrderAssignment
├── dto/           — CreateDriverRequest, DriverResponse, DriverAssignedEvent
└── order/dto/     — OrderCreatedEvent (копия из монолита для десериализации)
```

**Важно:** пакет `com.kopach.logistics.order.dto.OrderCreatedEvent` в driver-service — **точная копия** одноимённого класса из монолита. Kafka-заголовок указывает полное имя класса-источника, и Jackson ищет его в classpath. Разные пакеты = `ClassNotFoundException`.

## Лицензия

MIT