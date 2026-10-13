package com.kopach.logistics.driver.dto;

import com.kopach.logistics.driver.entity.Driver;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DriverResponse {

    private Long id;
    private String name;
    private String phone;
    private String status;
    private BigDecimal rating;
    private LocalDateTime createdAt;

    public DriverResponse() {
    }

    public static DriverResponse from(Driver driver) {
        DriverResponse r = new DriverResponse();
        r.id = driver.getId();
        r.name = driver.getName();
        r.phone = driver.getPhone();
        r.status = driver.getStatus().name();
        r.rating = driver.getRating();
        r.createdAt = driver.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getStatus() { return status; }
    public BigDecimal getRating() { return rating; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}