package com.kopach.logistics.driver.repository;

import com.kopach.logistics.driver.entity.Driver;
import com.kopach.logistics.driver.entity.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findFirstByStatus(DriverStatus status);

    List<Driver> findAllByStatus(DriverStatus status);
}