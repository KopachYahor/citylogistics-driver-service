package com.kopach.logistics.driver.kafka;

import com.kopach.logistics.driver.dto.DriverAssignedEvent;
import com.kopach.logistics.driver.entity.Driver;
import com.kopach.logistics.driver.entity.DriverStatus;
import com.kopach.logistics.driver.entity.OrderAssignment;
import com.kopach.logistics.driver.repository.DriverRepository;
import com.kopach.logistics.driver.repository.OrderAssignmentRepository;
import com.kopach.logistics.order.dto.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты назначения водителя на новый заказ (order.created).
 */
@ExtendWith(MockitoExtension.class)
class OrderCreatedListenerTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private OrderAssignmentRepository assignmentRepository;

    @Mock
    private DriverAssignedPublisher publisher;

    @InjectMocks
    private OrderCreatedListener listener;

    @Test
    void newOrder_shouldAssignAvailableDriverAndPublishEvent() {
        Driver driver = driver(5L, DriverStatus.AVAILABLE);
        when(assignmentRepository.existsByOrderId(100L)).thenReturn(false);
        when(driverRepository.findFirstByStatus(DriverStatus.AVAILABLE)).thenReturn(Optional.of(driver));

        listener.handleOrderCreated(orderCreated(100L));

        // создано назначение заказ 100 → водитель 5
        ArgumentCaptor<OrderAssignment> assignment = ArgumentCaptor.forClass(OrderAssignment.class);
        verify(assignmentRepository).save(assignment.capture());
        assertThat(assignment.getValue().getOrderId()).isEqualTo(100L);
        assertThat(assignment.getValue().getDriverId()).isEqualTo(5L);

        // водитель занят, core получает driver.assigned
        assertThat(driver.getStatus()).isEqualTo(DriverStatus.BUSY);
        ArgumentCaptor<DriverAssignedEvent> event = ArgumentCaptor.forClass(DriverAssignedEvent.class);
        verify(publisher).publish(event.capture());
        assertThat(event.getValue().getOrderId()).isEqualTo(100L);
        assertThat(event.getValue().getDriverId()).isEqualTo(5L);
    }

    @Test
    void repeatedEvent_shouldNotCreateSecondAssignment() {
        // идемпотентность: Kafka доставила order.created второй раз
        when(assignmentRepository.existsByOrderId(100L)).thenReturn(true);

        listener.handleOrderCreated(orderCreated(100L));

        verify(driverRepository, never()).findFirstByStatus(any());
        verify(assignmentRepository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void noAvailableDriver_shouldDoNothing() {
        when(assignmentRepository.existsByOrderId(100L)).thenReturn(false);
        when(driverRepository.findFirstByStatus(DriverStatus.AVAILABLE)).thenReturn(Optional.empty());

        listener.handleOrderCreated(orderCreated(100L));

        // заказ остаётся NEW: повторной попытки нет (известное ограничение, см. README)
        verify(assignmentRepository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    private OrderCreatedEvent orderCreated(Long orderId) {
        return new OrderCreatedEvent(orderId, 1L, "Minsk", "Brest", BigDecimal.TEN, "NEW", LocalDateTime.now());
    }

    private Driver driver(Long id, DriverStatus status) {
        Driver driver = new Driver(10L, "Test Driver", "+375290000000", status);
        driver.setId(id);
        return driver;
    }
}
