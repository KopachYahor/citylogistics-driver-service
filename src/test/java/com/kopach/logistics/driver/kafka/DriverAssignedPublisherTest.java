package com.kopach.logistics.driver.kafka;

import com.kopach.logistics.driver.dto.DriverAssignedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DriverAssignedPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final DriverAssignedPublisher publisher = new DriverAssignedPublisher(kafkaTemplate);

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void insideTransaction_shouldSendOnlyAfterCommit() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());
        TransactionSynchronizationManager.initSynchronization(); // как внутри @Transactional

        publisher.publish(new DriverAssignedEvent(100L, 7L));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());

        // транзакция закоммитилась
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(kafkaTemplate).send(eq("driver.assigned"), eq("100"), any(DriverAssignedEvent.class));
    }

    @Test
    void kafkaFailure_shouldBeLoggedNotThrown() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenThrow(new RuntimeException("broker is down"));

        assertThatCode(() -> publisher.publish(new DriverAssignedEvent(100L, 7L))).doesNotThrowAnyException();
    }
}
