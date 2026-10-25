package com.kopach.logistics.driver.kafka;

import com.kopach.logistics.order.dto.OrderStatusChangedEvent;
import com.kopach.logistics.driver.entity.Driver;
import com.kopach.logistics.driver.entity.DriverStatus;
import com.kopach.logistics.driver.repository.DriverRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderStatusChangedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusChangedListener.class);

    private final DriverRepository driverRepository;

    public OrderStatusChangedListener(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @KafkaListener(topics = "order.status-changed", groupId = "driver-service-group")
    @Transactional
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Received order.status-changed: orderId={}, driverId={}, {} -> {}",
                event.getOrderId(), event.getDriverId(), event.getOldStatus(), event.getNewStatus());

        boolean orderFinished = "DELIVERED".equals(event.getNewStatus())
                || "CANCELLED".equals(event.getNewStatus());

        if (!orderFinished) {
            return;
        }

        if (event.getDriverId() == null) {
            return;
        }

        Driver driver = driverRepository.findById(event.getDriverId()).orElse(null);
        if (driver == null) {
            log.warn("Driver not found: {}", event.getDriverId());
            return;
        }

        if (driver.getStatus() == DriverStatus.BUSY) {
            driver.setStatus(DriverStatus.AVAILABLE);
            driverRepository.save(driver);
            log.info("Driver {} is now AVAILABLE (order {} {})",
                    driver.getId(), event.getOrderId(), event.getNewStatus());
        }
    }
}