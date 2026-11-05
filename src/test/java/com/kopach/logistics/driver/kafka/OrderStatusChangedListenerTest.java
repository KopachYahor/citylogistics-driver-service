package com.kopach.logistics.driver.kafka;

import com.kopach.logistics.driver.entity.Driver;
import com.kopach.logistics.driver.entity.DriverStatus;
import com.kopach.logistics.driver.entity.OrderAssignment;
import com.kopach.logistics.driver.repository.DriverRepository;
import com.kopach.logistics.driver.repository.OrderAssignmentRepository;
import com.kopach.logistics.order.dto.OrderStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты освобождения водителя (order.status-changed).
 */
@ExtendWith(MockitoExtension.class)
class OrderStatusChangedListenerTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private OrderAssignmentRepository assignmentRepository;

    @InjectMocks
    private OrderStatusChangedListener listener;

    @Test
    void deliveredCurrentOrder_shouldReleaseDriver() {
        Driver driver = busyDriver(5L);
        when(driverRepository.findById(5L)).thenReturn(Optional.of(driver));
        when(assignmentRepository.findFirstByDriverIdOrderByIdDesc(5L))
                .thenReturn(Optional.of(new OrderAssignment(100L, 5L)));

        listener.onOrderStatusChanged(new OrderStatusChangedEvent(100L, 5L, "ARRIVED", "DELIVERED"));

        assertThat(driver.getStatus()).isEqualTo(DriverStatus.AVAILABLE);
        verify(driverRepository).save(driver);
    }

    @Test
    void cancelledCurrentOrder_shouldReleaseDriver() {
        Driver driver = busyDriver(5L);
        when(driverRepository.findById(5L)).thenReturn(Optional.of(driver));
        when(assignmentRepository.findFirstByDriverIdOrderByIdDesc(5L))
                .thenReturn(Optional.of(new OrderAssignment(100L, 5L)));

        listener.onOrderStatusChanged(new OrderStatusChangedEvent(100L, 5L, "CANCELLED", "CANCELLED"));

        assertThat(driver.getStatus()).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    void oldEventWhileDriverOnAnotherOrder_shouldKeepDriverBusy() {
        // повторное событие про старый заказ 7, а водитель уже везёт заказ 100
        Driver driver = busyDriver(5L);
        when(driverRepository.findById(5L)).thenReturn(Optional.of(driver));
        when(assignmentRepository.findFirstByDriverIdOrderByIdDesc(5L))
                .thenReturn(Optional.of(new OrderAssignment(100L, 5L)));

        listener.onOrderStatusChanged(new OrderStatusChangedEvent(7L, 5L, "ARRIVED", "DELIVERED"));

        assertThat(driver.getStatus()).isEqualTo(DriverStatus.BUSY);
        verify(driverRepository, never()).save(any());
    }

    @Test
    void notFinalStatus_shouldDoNothing() {
        listener.onOrderStatusChanged(new OrderStatusChangedEvent(100L, 5L, "ACCEPTED", "IN_TRANSIT"));

        verifyNoInteractions(driverRepository, assignmentRepository);
    }

    @Test
    void eventWithoutDriver_shouldDoNothing() {
        // отмена заказа, которому водителя ещё не назначили
        listener.onOrderStatusChanged(new OrderStatusChangedEvent(100L, null, "NEW", "CANCELLED"));

        verifyNoInteractions(driverRepository, assignmentRepository);
    }

    private Driver busyDriver(Long id) {
        Driver driver = new Driver(10L, "Test Driver", "+375290000000", DriverStatus.BUSY);
        driver.setId(id);
        return driver;
    }
}
