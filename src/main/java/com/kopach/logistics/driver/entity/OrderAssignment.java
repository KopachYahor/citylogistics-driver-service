package com.kopach.logistics.driver.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_assignments")
public class OrderAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    public OrderAssignment() {
    }

    public OrderAssignment(Long orderId, Long driverId) {
        this.orderId = orderId;
        this.driverId = driverId;
        this.assignedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
}