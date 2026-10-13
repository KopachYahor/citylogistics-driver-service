package com.kopach.logistics.driver.repository;

import com.kopach.logistics.driver.entity.OrderAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Long> {

    boolean existsByOrderId(Long orderId);
}