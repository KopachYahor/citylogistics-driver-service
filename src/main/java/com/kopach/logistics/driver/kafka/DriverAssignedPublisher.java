package com.kopach.logistics.driver.kafka;

import com.kopach.logistics.driver.dto.DriverAssignedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class DriverAssignedPublisher {

    private static final Logger log = LoggerFactory.getLogger(DriverAssignedPublisher.class);
    private static final String TOPIC = "driver.assigned";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DriverAssignedPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Слушатель вызывает это внутри @Transactional. Отправляем только после коммита:
    // если транзакция откатится (например, UNIQUE на order_id), core не получит назначение,
    // которого нет в базе. Если Kafka потеряет событие, его восстановит повтор order.created
    // от core: OrderCreatedListener заново отправит уже сделанное назначение
    public void publish(DriverAssignedEvent event) {
        log.info("Publishing driver.assigned: orderId={}, driverId={}", event.getOrderId(), event.getDriverId());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
        } else {
            send(event);
        }
    }

    private void send(DriverAssignedEvent event) {
        String key = event.getOrderId().toString();
        try {
            kafkaTemplate.send(TOPIC, key, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka did not accept driver.assigned for order {}: {}", key, ex.toString());
                }
            });
        } catch (RuntimeException e) {
            // send() бросает сразу, если брокер недоступен дольше max.block.ms
            log.error("Could not send driver.assigned for order {}: {}", key, e.toString());
        }
    }
}
