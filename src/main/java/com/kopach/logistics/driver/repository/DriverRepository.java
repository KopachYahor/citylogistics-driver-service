package com.kopach.logistics.driver.repository;

import com.kopach.logistics.driver.entity.Driver;
import com.kopach.logistics.driver.entity.DriverStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    // SELECT ... FOR UPDATE: два экземпляра сервиса не возьмут одного и того же свободного водителя
    // для разных заказов. Второй ждёт коммита первого. Работает только внутри @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Driver> findFirstByStatus(DriverStatus status);

    List<Driver> findAllByStatus(DriverStatus status);
}