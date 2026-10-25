package com.kopach.logistics.driver.controller;

import com.kopach.logistics.driver.dto.CreateDriverRequest;
import com.kopach.logistics.driver.dto.DriverResponse;
import com.kopach.logistics.driver.entity.Driver;
import com.kopach.logistics.driver.entity.DriverStatus;
import com.kopach.logistics.driver.repository.DriverRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverRepository driverRepository;

    public DriverController(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @GetMapping
    public List<DriverResponse> getAll() {
        return driverRepository.findAll().stream()
                .map(DriverResponse::from)
                .toList();
    }

    @GetMapping("/available")
    public List<DriverResponse> getAvailable() {
        return driverRepository.findAllByStatus(DriverStatus.AVAILABLE).stream()
                .map(DriverResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getById(@PathVariable Long id) {
        return driverRepository.findById(id)
                .map(DriverResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DriverResponse> create(@RequestBody @Valid CreateDriverRequest request) {
        Driver driver = new Driver(
                request.getUserId(),
                request.getName(),
                request.getPhone(),
                DriverStatus.AVAILABLE
        );
        Driver saved = driverRepository.save(driver);
        return ResponseEntity.ok(DriverResponse.from(saved));
    }
}