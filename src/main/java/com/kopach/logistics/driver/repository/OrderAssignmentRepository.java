package com.kopach.logistics.driver.repository;

import com.kopach.logistics.driver.entity.OrderAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Long> {

    // order_id уникален (V3), поэтому назначение у заказа одно
    Optional<OrderAssignment> findByOrderId(Long orderId);

    // Последнее назначение — текущий заказ водителя (id растёт)
    Optional<OrderAssignment> findFirstByDriverIdOrderByIdDesc(Long driverId);
}