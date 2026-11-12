package com.kopach.logistics.driver.repository;

import com.kopach.logistics.driver.entity.OrderAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Long> {

    boolean existsByOrderId(Long orderId);

    // Последнее назначение — текущий заказ водителя (id растёт)
    Optional<OrderAssignment> findFirstByDriverIdOrderByIdDesc(Long driverId);
}