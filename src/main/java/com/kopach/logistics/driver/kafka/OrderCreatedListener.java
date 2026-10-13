package com.kopach.logistics.driver.kafka;

import com.kopach.logistics.driver.dto.DriverAssignedEvent;
import com.kopach.logistics.order.dto.OrderCreatedEvent;
import com.kopach.logistics.driver.entity.Driver;
import com.kopach.logistics.driver.entity.DriverStatus;
import com.kopach.logistics.driver.entity.OrderAssignment;
import com.kopach.logistics.driver.repository.DriverRepository;
import com.kopach.logistics.driver.repository.OrderAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final DriverRepository driverRepository;
    private final OrderAssignmentRepository assignmentRepository;
    private final DriverAssignedPublisher publisher;

    public OrderCreatedListener(DriverRepository driverRepository,
                                OrderAssignmentRepository assignmentRepository,
                                DriverAssignedPublisher publisher) {
        this.driverRepository = driverRepository;
        this.assignmentRepository = assignmentRepository;
        this.publisher = publisher;
    }

    @KafkaListener(topics = "order.created", groupId = "driver-service-group")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order.created: orderId={}, from={}, to={}",
                event.getOrderId(), event.getFromAddress(), event.getToAddress());

        // идемпотентность: если заказ уже назначен — выходим
        if (assignmentRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Order {} already assigned, skipping", event.getOrderId());
            return;
        }

        // ищем свободного водителя
        Driver driver = driverRepository.findFirstByStatus(DriverStatus.AVAILABLE)
                .orElse(null);

        if (driver == null) {
            log.warn("No available driver for order {}", event.getOrderId());
            return;
        }

        // создаём назначение
        OrderAssignment assignment = new OrderAssignment(event.getOrderId(), driver.getId());
        assignmentRepository.save(assignment);

        // помечаем водителя BUSY
        driver.setStatus(DriverStatus.BUSY);
        driverRepository.save(driver);

        log.info("Assigned driver {} to order {}", driver.getId(), event.getOrderId());

        // публикуем событие driver.assigned
        publisher.publish(new DriverAssignedEvent(event.getOrderId(), driver.getId()));
    }
}