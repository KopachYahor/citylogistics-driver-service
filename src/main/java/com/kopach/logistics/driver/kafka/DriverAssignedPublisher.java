package com.kopach.logistics.driver.kafka;

import com.kopach.logistics.driver.dto.DriverAssignedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DriverAssignedPublisher {

    private static final Logger log = LoggerFactory.getLogger(DriverAssignedPublisher.class);
    private static final String TOPIC = "driver.assigned";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DriverAssignedPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DriverAssignedEvent event) {
        log.info("Publishing driver.assigned: orderId={}, driverId={}", event.getOrderId(), event.getDriverId());
        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);
    }
}